package ctmn.petals.net

import com.badlogic.gdx.utils.Queue
import ctmn.petals.map.MapConverted
import ctmn.petals.map.loadMap
import ctmn.petals.multiplayer.json.GameStateSnapshot
import ctmn.petals.net.proto.*
import ctmn.petals.playscreen.CaptureBases
import ctmn.petals.playscreen.GameMode
import ctmn.petals.playscreen.Season
import ctmn.petals.player.Player
import ctmn.petals.server.ServerConfig
import ctmn.petals.sim.BattleSetup
import ctmn.petals.sim.ServerBattle
import ctmn.petals.utils.GameLog
import ctmn.petals.utils.fromGson
import ctmn.petals.utils.toGson
import io.netty.bootstrap.ServerBootstrap
import io.netty.channel.*
import io.netty.channel.nio.NioEventLoopGroup
import io.netty.channel.socket.SocketChannel
import io.netty.channel.socket.nio.NioServerSocketChannel
import io.netty.handler.codec.LengthFieldBasedFrameDecoder
import io.netty.handler.codec.LengthFieldPrepender
import io.netty.handler.codec.string.StringDecoder
import io.netty.handler.codec.string.StringEncoder
import java.nio.charset.StandardCharsets
import java.util.UUID
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.ConcurrentLinkedQueue

/**
 * Authoritative dedicated server: connections, lobby registry, quick match queue and
 * running matches. A single worker thread owns all game state, so no battle is ever mutated
 * from a netty IO thread. Incoming frames are parsed on IO threads and queued here.
 */
class ServerCore(val config: ServerConfig) {

    private class ClientConnection(val channel: Channel) {
        var clientId: String = ""
        var name: String = "Player"
        var lobbyId: String? = null
        var matchId: String? = null
        var playerId: Int = -1
        var ready: Boolean = false

        /** Set when the connection dropped; zero while connected. */
        var disconnectAt = 0L

        /** Simple flood guard: messages counted within the current second. */
        var messagesThisSecond = 0
    }

    private val clientsById = ConcurrentHashMap<String, ClientConnection>()
    private val incoming = ConcurrentLinkedQueue<Pair<ClientConnection, Envelope>>()

    private val lobbies = LinkedHashMap<String, Lobby>()
    private val matches = HashMap<String, Match>()
    private val queue = ArrayDeque<ClientConnection>()

    private var bossGroup: EventLoopGroup? = null
    private var workerGroup: EventLoopGroup? = null
    private var channel: Channel? = null
    private var sessionThread: Thread? = null

    @Volatile
    private var running = false

    val connectedCount: Int get() = clientsById.size
    val activeMatchCount: Int get() = matches.size
    val lobbyCount: Int get() = lobbies.size
    val queuedCount: Int get() = queue.size

    private companion object {
        /** Roughly ten times what a normal turn needs, so a flood is cut off fast. */
        const val maxMessagesPerSecond = 120
    }

    private fun resolveCommandClass(name: String): Class<*>? {
        // Newer clients send the full class name, older ones only the simple name.
        if (name.contains('.'))
            return runCatching { Class.forName(name) }.getOrNull()

        return runCatching { Class.forName("ctmn.petals.playscreen.commands.$name") }.getOrNull()
    }

    fun start() {
        running = true

        bossGroup = NioEventLoopGroup(1)
        workerGroup = NioEventLoopGroup(2)

        val bootstrap = ServerBootstrap()
            .group(bossGroup, workerGroup)
            .channel(NioServerSocketChannel::class.java)
            .childOption(ChannelOption.SO_KEEPALIVE, true)
            .childHandler(object : ChannelInitializer<SocketChannel>() {
                override fun initChannel(ch: SocketChannel) {
                    ch.pipeline()
                        .addLast(LengthFieldBasedFrameDecoder(1 shl 20, 0, 4, 0, 4))
                        .addLast(LengthFieldPrepender(4))
                        .addLast(StringDecoder(StandardCharsets.UTF_8))
                        .addLast(StringEncoder(StandardCharsets.UTF_8))
                        .addLast(NettyHandler())
                }
            })

        channel = bootstrap.bind(config.port).sync().channel()
        GameLog.info("ServerCore", "'${config.serverName}' listening on port ${config.port}")

        sessionThread = Thread({ sessionLoop() }, "server-session").apply {
            isDaemon = true
            start()
        }
    }

    fun stop() {
        running = false
        queue.clear()
        matches.values.forEach { it.stop() }
        matches.clear()
        channel?.close()
        bossGroup?.shutdownGracefully()
        workerGroup?.shutdownGracefully()
    }

    private fun sessionLoop() {
        var last = System.nanoTime()
        while (running) {
            while (true) {
                val next = incoming.poll() ?: break
                try {
                    handle(next.first, next.second)
                } catch (e: Exception) {
                    GameLog.error("ServerCore", "Failed handling ${next.second.type}", e)
                }
            }

            val now = System.nanoTime()
            val dt = ((now - last) / 1_000_000_000.0).toFloat().coerceAtMost(0.25f)
            last = now

            val finished = ArrayList<String>()
            matches.values.forEach { match ->
                try {
                    match.tick(dt)
                } catch (e: Exception) {
                    GameLog.error("ServerCore", "Match tick failed", e)
                }
                if (match.isFinished) finished.add(match.id)
            }
            finished.forEach { matches.remove(it) }

            reapTimedOutSeats()

            // Reset the per second message counters used by the flood guard.
            clientsById.values.forEach { it.messagesThisSecond = 0 }

            try {
                Thread.sleep(15)
            } catch (e: InterruptedException) {
                break
            }
        }
    }

    // Networking

    private inner class NettyHandler : SimpleChannelInboundHandler<String>() {

        override fun channelRead0(ctx: ChannelHandlerContext, msg: String) {
            val connection = ctx.channel().attr(connectionKey).get()
            val envelope = try {
                fromGson(msg, Envelope::class.java)
            } catch (e: Exception) {
                return
            }
            incoming.add(connection to envelope)
        }

        override fun channelActive(ctx: ChannelHandlerContext) {
            val connection = ClientConnection(ctx.channel())
            ctx.channel().attr(connectionKey).set(connection)
            super.channelActive(ctx)
        }

        override fun channelInactive(ctx: ChannelHandlerContext) {
            val connection = ctx.channel().attr(connectionKey).get()
            incoming.add(connection to Envelope().also { it.type = "__disconnect" })
            super.channelInactive(ctx)
        }
    }

    private val connectionKey = io.netty.util.AttributeKey.valueOf<ClientConnection>("petals.connection")

    // Dispatch

    private fun handle(connection: ClientConnection, envelope: Envelope) {
        // Every message is traced: a per command broadcast log would be far too noisy.
        GameLog.verbose("ServerCore", "recv ${envelope.type} from '${connection.name}'")

        // Flood guard: a client that floods gets dropped instead of stalling the session.
        if (++connection.messagesThisSecond > maxMessagesPerSecond) {
            sendError(connection, envelope.requestId, "Too many requests")
            connection.channel.close()
            return
        }

        when (envelope.type) {
            "__disconnect" -> onDisconnected(connection)
            Msg.HELLO -> onHello(connection, fromGson(envelope.payload, Hello::class.java))
            Msg.PING -> sendTo(connection, Msg.PONG, envelope.requestId, Pong().also { it.time = fromGson(envelope.payload, Ping::class.java).time })
            Msg.LIST_LOBBIES -> sendLobbyList(connection)
            Msg.CREATE_LOBBY -> onCreateLobby(connection, fromGson(envelope.payload, CreateLobby::class.java))
            Msg.JOIN_LOBBY -> onJoinLobby(connection, fromGson(envelope.payload, JoinLobby::class.java))
            Msg.LEAVE_LOBBY -> onLeaveLobby(connection)
            Msg.READY -> onReady(connection, fromGson(envelope.payload, Ready::class.java))
            Msg.START -> onStart(connection)
            Msg.QUEUE_JOIN -> onQueueJoin(connection)
            Msg.QUEUE_LEAVE -> onQueueLeave(connection)
            Msg.MATCH_COMMAND -> onMatchCommand(connection, envelope.requestId, fromGson(envelope.payload, MatchCommand::class.java))
            Msg.SNAPSHOT_REQUEST -> onSnapshotRequest(connection)
            else -> sendError(connection, envelope.requestId, "Unknown message type: ${envelope.type}")
        }
    }

    private fun onHello(connection: ClientConnection, hello: Hello) {
        val requestedId = if (hello.clientId.isBlank()) UUID.randomUUID().toString() else hello.clientId
        val name = hello.name.ifBlank { "Player${requestedId.take(4)}" }.take(24)

        // There must be exactly one live connection object per client: the one the netty
        // handler holds. Everything else (queue, lobbies, matches) is looked up by client id,
        // so a second object would hide the match seat from the connection that receives
        // commands.
        val previous = clientsById[requestedId]

        connection.clientId = requestedId
        connection.name = name

        if (previous != null) {
            // A returning player keeps their lobby or match seat.
            connection.lobbyId = previous.lobbyId
            connection.matchId = previous.matchId
            connection.playerId = previous.playerId
            connection.ready = previous.ready
            GameLog.info("ServerCore", "Player '${connection.name}' reconnected")
        } else {
            GameLog.info("ServerCore", "Player '${connection.name}' (${connection.clientId}) connected")
        }

        clientsById[requestedId] = connection
        val session = connection

        sendTo(session, Msg.WELCOME, "", Welcome().also {
            it.clientId = session.clientId
            it.name = session.name
            it.serverName = config.serverName
            it.mapIds.addAll(config.mapPool)
        })

        // Hand the fresh state of whatever they were in.
        session.matchId?.let { id -> matches[id]?.sendSnapshot(session) }
    }

    private fun onDisconnected(connection: ClientConnection) {
        if (connection.clientId.isEmpty()) return

        // A reconnect replaces the session object, so a late close of the old socket must
        // not clean up the new session.
        if (clientsById[connection.clientId] !== connection) return

        queue.remove(connection)

        connection.lobbyId?.let { id ->
            connection.lobbyId = null
            val lobby = lobbies[id]
            if (lobby != null) {
                lobby.members.removeIf { it.clientId == connection.clientId }
                if (lobby.members.isEmpty()) lobbies.remove(id)
                else {
                    if (lobby.host === connection) lobby.host = lobby.members.first()
                    broadcastLobby(lobby)
                }
            }
            broadcastLobbyList()
        }

        if (connection.matchId != null) {
            // Keep the seat for a while so a dropped connection can pick it back up.
            connection.disconnectAt = System.currentTimeMillis() + config.reconnectGraceSeconds * 1000L
            matches[connection.matchId]?.onPlayerDisconnected(connection)
        } else {
            clientsById.remove(connection.clientId)
        }
    }

    /** Drops seats whose grace period ran out. */
    private fun reapTimedOutSeats() {
        val now = System.currentTimeMillis()
        val expired = clientsById.values.filter { it.disconnectAt > 0L && it.disconnectAt <= now }

        for (session in expired) {
            GameLog.info("ServerCore", "Player '${session.name}' did not come back")
            session.matchId?.let { id -> matches[id]?.onPlayerGone(session) }
            clientsById.remove(session.clientId)
        }
    }

    // Lobbies

    private fun onCreateLobby(connection: ClientConnection, request: CreateLobby) {
        if (lobbies.size >= config.maxSessions) {
            sendError(connection, "", "Server is at its session limit")
            return
        }

        val lobby = Lobby(
            id = UUID.randomUUID().toString().take(8),
            name = request.name.ifBlank { "${connection.name}'s game" }.take(32),
            mapId = if (config.mapPool.contains(request.mapId)) request.mapId else config.mapPool.random(),
            host = connection,
            maxPlayers = request.maxPlayers.coerceIn(2, 8),
            fogOfWar = request.fogOfWar,
            gameMode = request.gameMode,
        )

        lobbies[lobby.id] = lobby
        connection.lobbyId = lobby.id
        connection.ready = true

        broadcastLobby(lobby)
        broadcastLobbyList()
    }

    private fun onJoinLobby(connection: ClientConnection, request: JoinLobby) {
        val lobby = lobbies[request.lobbyId]
        if (lobby == null) {
            sendError(connection, "", "Lobby not found")
            return
        }
        if (lobby.members.size >= lobby.maxPlayers) {
            sendError(connection, "", "Lobby is full")
            return
        }

        connection.lobbyId = lobby.id
        broadcastLobby(lobby)
        broadcastLobbyList()
    }

    private fun onLeaveLobby(connection: ClientConnection) {
        val id = connection.lobbyId ?: return
        connection.lobbyId = null
        connection.ready = false
        val lobby = lobbies[id] ?: return
        lobby.members.removeIf { it.clientId == connection.clientId }
        if (lobby.members.isEmpty()) lobbies.remove(id) else {
            if (lobby.host === connection) lobby.host = lobby.members.first()
            broadcastLobby(lobby)
        }
        broadcastLobbyList()
    }

    private fun onReady(connection: ClientConnection, ready: Ready) {
        connection.ready = ready.ready
        lobbies[connection.lobbyId]?.let { broadcastLobby(it) }
    }

    private fun onStart(connection: ClientConnection) {
        val lobby = lobbies[connection.lobbyId] ?: return
        if (lobby.host !== connection) {
            sendError(connection, "", "Only the host can start the match")
            return
        }

        startMatch(lobby.members.map { it }, lobby.mapId, lobby.fogOfWar, lobby.gameMode)
        lobbies.remove(lobby.id)
        broadcastLobbyList()
    }

    private fun broadcastLobby(lobby: Lobby) {
        val state = lobby.toState()
        lobby.members.forEach { member ->
            val connection = clientsById[member.clientId] ?: return@forEach
            sendTo(connection, Msg.LOBBY_STATE, "", state)
        }
    }

    private fun sendLobbyList(connection: ClientConnection) {
        val list = LobbyList()
        lobbies.values.forEach { list.lobbies.add(it.toInfo()) }
        sendTo(connection, Msg.LOBBY_LIST, "", list)
    }

    private fun broadcastLobbyList() {
        val list = LobbyList()
        lobbies.values.forEach { list.lobbies.add(it.toInfo()) }
        clientsById.values.forEach { sendTo(it, Msg.LOBBY_LIST, "", list) }
    }

    // Queue

    private fun onQueueJoin(connection: ClientConnection) {
        if (queue.contains(connection)) return
        if (connection.matchId != null || connection.lobbyId != null) {
            sendError(connection, "", "Leave your current lobby or match first")
            return
        }

        queue.addLast(connection)
        broadcastQueueStatus()

        if (queue.size >= 2) {
            val a = queue.removeFirst()
            val b = queue.removeFirst()
            startMatch(listOf(a, b), config.mapPool.random(), fogOfWar = true, gameMode = GameMode.CRYSTALS.name)
            broadcastQueueStatus()
        }
    }

    private fun onQueueLeave(connection: ClientConnection) {
        queue.remove(connection)
        broadcastQueueStatus()
    }

    private fun broadcastQueueStatus() {
        val list = queue.toList()
        list.forEachIndexed { index, connection ->
            sendTo(connection, Msg.QUEUE_STATUS, "", QueueStatus().also {
                it.position = index + 1
                it.size = list.size
            })
        }
    }

    // Matches

    private fun startMatch(members: List<ClientConnection>, mapId: String, fogOfWar: Boolean, gameMode: String) {
        val map: MapConverted = try {
            loadMap(mapId)
        } catch (e: Exception) {
            members.forEach { sendError(it, "", "Map '$mapId' not found") }
            return
        }

        val players = ArrayList<Player>()
        members.forEachIndexed { index, connection ->
            val player = Player(name = connection.name, id = index + 1, teamId = index + 1)
            player.clientId = connection.clientId
            players.add(player)
        }

        val battle = BattleSetup.build(
            map = map,
            players = players,
            gameEndCondition = CaptureBases(),
            gameMode = try { GameMode.valueOf(gameMode) } catch (e: Exception) { GameMode.CRYSTALS },
            season = Season.SUMMER,
            turnTimeLimitSeconds = config.turnTimeLimitSeconds,
        )
        battle.clientControlled = true

        val match = Match(UUID.randomUUID().toString().take(8), battle, fogOfWar)
        members.forEachIndexed { index, connection ->
            connection.matchId = match.id
            connection.playerId = players[index].id
            match.clientIds[players[index].id] = connection.clientId
        }
        matches[match.id] = match

        val mapJson = map.mapSave.toGson()

        members.forEachIndexed { index, connection ->
            val found = MatchFound()
            found.matchId = match.id
            found.you = players[index].id
            found.mapId = mapId
            found.mapJson = mapJson
            found.gameMode = battle.gameMode.name
            found.season = battle.season.name
            found.daytime = "DAY"
            found.fogOfWar = fogOfWar
            found.turnTimeLimitSeconds = battle.turnTimeLimitSeconds
            members.forEachIndexed { i, m ->
                found.players.add(LobbyMember().also {
                    it.clientId = m.clientId
                    it.name = m.name
                    it.playerId = players[i].id
                })
            }
            sendTo(connection, Msg.MATCH_FOUND, "", found)
            match.sendSnapshot(connection)
        }

        GameLog.info("ServerCore", "Match ${match.id} started on '$mapId' with ${members.size} players")
    }

    private fun onMatchCommand(connection: ClientConnection, requestId: String, request: MatchCommand) {
        val match = matches[connection.matchId] ?: return
        match.onCommand(connection, requestId, request)
    }

    private fun onSnapshotRequest(connection: ClientConnection) {
        GameLog.verbose("ServerCore", "Match ${connection.matchId}: ${connection.name} asked for a snapshot")
        matches[connection.matchId]?.sendSnapshot(connection)
    }

    private inner class Match(val id: String, val battle: ServerBattle, val fogOfWar: Boolean) {

        val clientIds = HashMap<Int, String>()
        var lastCommandTime = System.nanoTime()

        private var deadlinePlayerId = -1
        private var deadlineAt = 0L

        val isFinished: Boolean get() = battle.isGameOver

        init {
            battle.commandManager.onCommand = {
                battle.gameStateId++
                lastCommandTime = System.nanoTime()
            }
        }

        fun tick(dt: Float) {
            battle.update(dt)
            checkTurnTimeout()

            if (battle.isGameOver) {
                val ended = MatchEnded()
                ended.winners.addAll(battle.gameEndCondition.winners)
                connectionFor().forEach { sendTo(it, Msg.MATCH_ENDED, "", ended) }
            }
        }

        /**
         * Ends a stalled turn when the server was configured with a turn time limit.
         * The limit is off by default, so matches only auto advance when asked for.
         */
        private fun checkTurnTimeout() {
            val limit = battle.turnTimeLimitSeconds
            if (limit <= 0 || battle.isGameOver) return

            val current = battle.turnManager.currentPlayer.id

            if (current != deadlinePlayerId) {
                deadlinePlayerId = current
                deadlineAt = System.currentTimeMillis() + limit * 1000L
                return
            }

            if (System.currentTimeMillis() < deadlineAt) return

            deadlinePlayerId = -1

            val endTurn = ctmn.petals.playscreen.commands.EndTurnCommand(current)
            if (!endTurn.canExecute(battle)) return

            if (!endTurn.execute(battle)) return
            battle.commandManager.onCommand?.invoke(endTurn)

            val broadcast = MatchCommandBroadcast()
            broadcast.stateId = battle.gameStateId
            broadcast.playerId = current
            broadcast.commandName = endTurn.javaClass.name
            broadcast.commandJson = endTurn.toGson()
            connectionFor().forEach { sendTo(it, Msg.MATCH_COMMAND_BROADCAST, "", broadcast) }

            GameLog.info("ServerCore", "Match $id: player $current ran out of time")
        }

        fun onCommand(connection: ClientConnection, requestId: String, request: MatchCommand) {
            val expectedPlayerId = connection.playerId
            if (expectedPlayerId <= 0) {
                sendError(connection, requestId, "You are not in this match")
                return
            }

            if (request.expectedStateId != 0 && request.expectedStateId != battle.gameStateId) {
                reject(connection, requestId, "State out of date")
                return
            }

            val commandClass = resolveCommandClass(request.commandName)
            if (commandClass == null) {
                reject(connection, requestId, "Unknown command")
                return
            }

            val command = try {
                fromGson(request.commandJson, commandClass) as ctmn.petals.playscreen.commands.Command
            } catch (e: Exception) {
                reject(connection, requestId, "Bad command payload")
                return
            }

            command.playerId = expectedPlayerId
            if (command is ctmn.petals.playscreen.commands.EndTurnCommand)
                command.turnPlayerId = expectedPlayerId

            if (!command.canExecute(battle)) {
                reject(connection, requestId, "Command not allowed")
                return
            }

            val ok = command.execute(battle)
            if (!ok) {
                reject(connection, requestId, "Command failed")
                return
            }

            battle.commandManager.onCommand?.invoke(command)

            GameLog.info(
                "ServerCore",
                "Match $id: ${connection.name} -> ${command.javaClass.simpleName} (state ${battle.gameStateId})"
            )

            val broadcast = MatchCommandBroadcast()
            broadcast.stateId = battle.gameStateId
            broadcast.playerId = expectedPlayerId
            broadcast.commandName = command.javaClass.name
            broadcast.commandJson = request.commandJson
            connectionFor().forEach { sendTo(it, Msg.MATCH_COMMAND_BROADCAST, "", broadcast) }

            // No snapshot here on purpose. The broadcast stream is ordered and reliable, so a
            // client that applies every command stays in sync on its own. A full snapshot is
            // only sent when a client really needs one: a rejection, a state gap, or a
            // reconnect. Sending one per command made every single action cost a full state
            // transfer.
        }

        private fun reject(connection: ClientConnection, requestId: String, reason: String) {
            GameLog.info("ServerCore", "Match $id: rejected '${connection.name}': $reason")

            sendTo(connection, Msg.COMMAND_REJECTED, requestId, CommandRejected().also {
                it.requestId = requestId
                it.reason = reason
            })
            sendSnapshot(connection)
        }

        fun onPlayerDisconnected(connection: ClientConnection) {
            connectionFor().forEach { other ->
                sendError(other, "", "Connection to '${connection.name}' lost, waiting for reconnect")
            }
            // The match keeps running; a reconnecting player picks their seat back up.
        }

        fun onPlayerGone(connection: ClientConnection) {
            connectionFor().forEach { other ->
                sendError(other, "", "Player '${connection.name}' left the match")
            }
        }

        fun sendSnapshot(connection: ClientConnection) {
            val snapshot = GameStateSnapshot(battle)

            if (fogOfWar) {
                val viewerId = clientIds.entries.firstOrNull { it.value == connection.clientId }?.key
                val viewer = viewerId?.let { battle.turnManager.getPlayerById(it) }
                if (viewer != null) {
                    val visibility = ctmn.petals.sim.FogOfWar.compute(
                        battle.field, battle.turnManager.players, viewer
                    )
                    SnapshotFilter.apply(snapshot, visibility, viewer.teamId)
                }
            }

            val message = MatchSnapshot()
            message.stateId = battle.gameStateId
            message.snapshotJson = snapshot.toGson()
            sendTo(connection, Msg.MATCH_SNAPSHOT, "", message)
        }

        private fun connectionFor(): List<ClientConnection> =
            clientIds.values.mapNotNull { clientsById[it] }

        fun stop() {}
    }

    private inner class Lobby(
        val id: String,
        val name: String,
        var mapId: String,
        var host: ClientConnection,
        val maxPlayers: Int,
        val fogOfWar: Boolean,
        val gameMode: String,
    ) {
        val members = ArrayList<ClientConnection>()

        init {
            members.add(host)
        }

        fun toInfo(): LobbyInfo = LobbyInfo().also {
            it.id = id
            it.name = name
            it.mapId = mapId
            it.hostName = host.name
            it.players = members.size
            it.maxPlayers = maxPlayers
            it.state = "WAITING"
        }

        fun toState(): LobbyStateMsg = LobbyStateMsg().also { state ->
            state.id = id
            state.name = name
            state.hostClientId = host.clientId
            state.mapId = mapId
            state.gameMode = gameMode
            state.fogOfWar = fogOfWar
            state.maxPlayers = maxPlayers
            state.state = if (members.all { it.ready || it === host }) "STARTING" else "WAITING"
            members.forEachIndexed { index, member ->
                state.members.add(LobbyMember().also {
                    it.clientId = member.clientId
                    it.name = member.name
                    it.playerId = index + 1
                    it.ready = member.ready || member === host
                    it.host = member === host
                })
            }
        }
    }

    // Outbound helpers

    private fun sendTo(connection: ClientConnection, type: String, requestId: String, payload: Any) {
        val envelope = Envelope()
        envelope.type = type
        envelope.requestId = requestId
        envelope.payload = if (payload is String) payload else payload.toGson()
        connection.channel.writeAndFlush(envelope.toGson())
    }

    private fun sendError(connection: ClientConnection, requestId: String, message: String) {
        sendTo(connection, Msg.ERROR, requestId, ErrorMsg().also {
            it.requestId = requestId
            it.message = message
        })
    }
}
