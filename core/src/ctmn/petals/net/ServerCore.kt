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
        connection.clientId = if (hello.clientId.isBlank()) UUID.randomUUID().toString() else hello.clientId
        connection.name = hello.name.ifBlank { "Player${connection.clientId.take(4)}" }.take(24)
        clientsById[connection.clientId] = connection

        GameLog.info("ServerCore", "Player '${connection.name}' (${connection.clientId}) connected")

        sendTo(connection, Msg.WELCOME, "", Welcome().also {
            it.clientId = connection.clientId
            it.name = connection.name
            it.serverName = config.serverName
        })
    }

    private fun onDisconnected(connection: ClientConnection) {
        if (connection.clientId.isNotEmpty()) {
            clientsById.remove(connection.clientId)
            GameLog.info("ServerCore", "Player '${connection.name}' disconnected")
        }

        queue.remove(connection)
        connection.lobbyId?.let { id ->
            lobbies[id]?.let { lobby ->
                lobby.members.removeIf { it.clientId == connection.clientId }
                if (lobby.members.isEmpty()) lobbies.remove(id) else broadcastLobby(lobby)
            }
        }
        connection.matchId?.let { id ->
            matches[id]?.let { match ->
                match.onPlayerDisconnected(connection)
            }
        }
        broadcastLobbyList()
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
            mapId = request.mapId.ifBlank { config.mapPool.random() },
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
        )
        battle.clientControlled = true

        val match = Match(UUID.randomUUID().toString().take(8), battle)
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
        matches[connection.matchId]?.sendSnapshot(connection)
    }

    private inner class Match(val id: String, val battle: ServerBattle) {

        val clientIds = HashMap<Int, String>()
        var lastCommandTime = System.nanoTime()

        val isFinished: Boolean get() = battle.isGameOver

        init {
            battle.commandManager.onCommand = {
                battle.gameStateId++
                lastCommandTime = System.nanoTime()
            }
        }

        fun tick(dt: Float) {
            battle.update(dt)

            if (battle.isGameOver) {
                val ended = MatchEnded()
                ended.winners.addAll(battle.gameEndCondition.winners)
                connectionFor().forEach { sendTo(it, Msg.MATCH_ENDED, "", ended) }
            }
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

            val commandClass = try {
                Class.forName("ctmn.petals.playscreen.commands.${request.commandName}")
            } catch (e: Exception) {
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

            val broadcast = MatchCommandBroadcast()
            broadcast.stateId = battle.gameStateId
            broadcast.playerId = expectedPlayerId
            broadcast.commandName = request.commandName
            broadcast.commandJson = request.commandJson
            connectionFor().forEach { sendTo(it, Msg.MATCH_COMMAND_BROADCAST, "", broadcast) }

            // Nudge the player whose turn just started with a fresh snapshot.
            clientIds[battle.turnManager.currentPlayer.id]?.let { id ->
                clientsById[id]?.let { sendSnapshot(it) }
            }
        }

        private fun reject(connection: ClientConnection, requestId: String, reason: String) {
            sendTo(connection, Msg.COMMAND_REJECTED, requestId, CommandRejected().also {
                it.requestId = requestId
                it.reason = reason
            })
            sendSnapshot(connection)
        }

        fun onPlayerDisconnected(connection: ClientConnection) {
            connectionFor().forEach { other ->
                sendError(other, "", "Player '${connection.name}' left the match")
            }
            // The match keeps running; a disconnected player simply stops acting.
        }

        fun sendSnapshot(connection: ClientConnection) {
            val snapshot = MatchSnapshot()
            snapshot.stateId = battle.gameStateId
            snapshot.snapshotJson = GameStateSnapshot(battle).toGson()
            sendTo(connection, Msg.MATCH_SNAPSHOT, "", snapshot)
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
