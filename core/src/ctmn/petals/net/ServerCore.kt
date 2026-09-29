package ctmn.petals.net

import com.badlogic.gdx.utils.Queue
import ctmn.petals.bot.v2.BotFactory
import ctmn.petals.bot.v2.Difficulty
import ctmn.petals.map.MapConverted
import ctmn.petals.map.createMapFromJson
import ctmn.petals.map.labels
import ctmn.petals.map.loadMap
import ctmn.petals.multiplayer.json.GameStateSnapshot
import ctmn.petals.net.proto.*
import ctmn.petals.playscreen.CaptureBases
import ctmn.petals.playscreen.GameMode
import ctmn.petals.playscreen.Season
import ctmn.petals.playstage.findMovePath
import ctmn.petals.playstage.getCapturablesOf
import ctmn.petals.playstage.reachableMoveTiles
import ctmn.petals.tile.isBase
import ctmn.petals.tile.isPassableAndFree
import ctmn.petals.unit.canMove
import ctmn.petals.unit.isAlly
import ctmn.petals.unit.playerId
import ctmn.petals.unit.tiledX
import ctmn.petals.unit.tiledY
import ctmn.petals.player.Player
import ctmn.petals.player.Species
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
import java.net.SocketException
import java.nio.channels.ClosedChannelException
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

        /** The player's own species, sent when they join so the lobby can show it. */
        var species: String = ""

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

    /**
     * Debug aid: when above zero, the first client to join the quick match queue is seated
     * against this many bots instead of waiting for a second human. See
     * `botmatchonline` in [ctmn.petals.server.ServerConfig].
     */
    var botMatchOnFirstQueueJoin = 0
    var botMatchDifficulty = "normal"

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

        /** Lobby seats. The client's setup screen has the same number. */
        const val SLOT_COUNT = 8
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
        matches.values.forEach { it.stop("server shutting down") }
        matches.clear()
        channel?.close()
        bossGroup?.shutdownGracefully()
        workerGroup?.shutdownGracefully()
    }

    /**
     * One thread handles every connection, so nothing here may end it. A single bad message must
     * cost that message and nothing else: an Error such as a missing class used to kill the loop
     * outright, leaving a server that still listened but never answered again.
     */
    private fun sessionLoop() {
        var last = System.nanoTime()
        while (running) {
            while (true) {
                val next = incoming.poll() ?: break
                try {
                    handle(next.first, next.second)
                } catch (t: Throwable) {
                    GameLog.error("ServerCore", "Failed handling ${next.second.type}", t)
                }
            }

            val now = System.nanoTime()
            val dt = ((now - last) / 1_000_000_000.0).toFloat().coerceAtMost(0.25f)
            last = now

            val finished = ArrayList<String>()
            matches.values.forEach { match ->
                try {
                    match.tick(dt)
                } catch (t: Throwable) {
                    GameLog.error("ServerCore", "Match tick failed", t)
                }

                // A match with nobody left in it has no reason to keep playing. Left running it
                // would tick forever and, worse, keep broadcasting to a connection that has already
                // joined another match.
                if (match.hasNoMembersLeft()) {
                    match.stop("nobody left")
                }

                if (match.isFinished || match.finished) finished.add(match.id)
            }
            finished.forEach { id ->
                matches.remove(id)

                // A lobby that showed as in game is over with its match.
                val lobby = lobbies.values.firstOrNull { it.matchId == id }
                if (lobby != null) {
                    lobbies.remove(lobby.id)
                    broadcastLobbyList()
                }
            }

            // Nothing broke the loop above, but the loop as a whole is what the server is: a
            // failure in the housekeeping must not leave a server that listens and never answers.
            try {
                reapTimedOutSeats()

                // Reset the per second message counters used by the flood guard.
                clientsById.values.forEach { it.messagesThisSecond = 0 }
            } catch (t: Throwable) {
                GameLog.error("ServerCore", "Session housekeeping failed", t)
            }

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

        override fun exceptionCaught(ctx: ChannelHandlerContext, cause: Throwable) {
            // A client that vanishes mid game resets the TCP connection, which netty reports here.
            // Without this it reaches the tail of the pipeline and prints a full stack trace for
            // what is just somebody closing the window. Anything else is a real error.
            val routine = generateSequence<Throwable>(cause) { it.cause }.any {
                it is SocketException || it is ClosedChannelException ||
                        it is java.io.EOFException ||
                        (it is java.io.IOException &&
                                it.message?.contains("Broken pipe", ignoreCase = true) == true)
            }

            if (!routine) GameLog.error("ServerCore", "Channel error", cause)
            ctx.close()
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
            Msg.MOVE_SEAT -> onMoveSeat(connection, fromGson(envelope.payload, MoveSeat::class.java))
            Msg.READY -> onReady(connection, fromGson(envelope.payload, Ready::class.java))
            Msg.START -> onStart(connection)
            Msg.QUEUE_JOIN -> onQueueJoin(connection)
            Msg.QUEUE_LEAVE -> onQueueLeave(connection)
            Msg.MATCH_COMMAND -> onMatchCommand(connection, envelope.requestId, fromGson(envelope.payload, MatchCommand::class.java))
            Msg.SNAPSHOT_REQUEST -> onSnapshotRequest(connection)
            Msg.LEAVE_MATCH -> onLeaveMatch(connection)
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

        removeFromLobby(connection)

        if (connection.matchId.isNullOrBlank()) {
            clientsById.remove(connection.clientId)
        } else {
            // Keep the seat for a while so a dropped connection can pick it back up.
            connection.disconnectAt = System.currentTimeMillis() + config.reconnectGraceSeconds * 1000L
            matches[connection.matchId]?.onPlayerDisconnected(connection)
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

    /**
     * Creates the host's lobby, or updates the one they already have.
     *
     * Creating is an upsert, not a one shot: the host opens the lobby once, and every later change
     * they make comes back through here so it reaches the players already sitting in it. That is
     * how the LAN flow behaved, where a lobby was live from the moment it existed.
     */
    private fun onCreateLobby(connection: ClientConnection, request: CreateLobby) {
        // Hosting again means the previous match, if any, is over for this player.
        leaveCurrentMatch(connection, "'${connection.name}' went back to hosting")

        val existing = connection.lobbyId?.let { lobbies[it] }?.takeIf { it.host === connection }
        val lobby = existing ?: run {
            if (lobbies.size >= config.maxSessions) {
                sendError(connection, "", "Server is at its session limit")
                return
            }

            Lobby(
                id = UUID.randomUUID().toString().take(8),
                name = request.name.ifBlank { "${connection.name}'s game" }.take(32),
                // The host's map id, taken as it is, blank included: a lobby exists before its
                // map is chosen and only a match needs one. The map itself travels in `mapJson`
                // when it only exists on the host's machine.
                mapId = request.mapId,
                mapName = request.mapName,
                host = connection,
                maxPlayers = request.maxPlayers.coerceIn(2, 8),
                fogOfWar = request.fogOfWar,
                gameMode = request.gameMode,
                hostMapJson = request.mapJson.takeIf { it.isNotBlank() },
            ).also {
                lobbies[it.id] = it
                connection.lobbyId = it.id
            }
        }

        // Whatever the host sent is what the lobby is now, including on an update.
        lobby.name = request.name.ifBlank { lobby.name }.take(32)
        lobby.mapId = request.mapId.ifBlank { lobby.mapId }
        lobby.mapName = request.mapName.ifBlank { lobby.mapName }
        lobby.fogOfWar = request.fogOfWar
        lobby.gameMode = request.gameMode
        lobby.season = request.season
        lobby.daytime = request.daytime
        lobby.listed = request.listed
        request.mapJson.takeIf { it.isNotBlank() }?.let { lobby.hostMapJson = it }

        // The host's arrangement: their own seat, everybody else's seat and the bots, each with
        // the id the lobby already gave it, so colours survive. The host is found by client id and
        // never by "the first seat that is not a bot": with a joiner in an earlier slot that
        // handed the host the joiner's id, which is what changed the host's colour.
        var hostSeatSeen = false
        request.seats.forEach { seat ->
            val clientId = seat.clientId

            when {
                clientId.isBlank() -> Unit
                clientId == connection.clientId -> {
                    lobby.hostSeat = seat
                    hostSeatSeen = true
                }

                lobby.members.any { it.clientId == clientId } -> lobby.seatsByClient[clientId] = seat
            }
        }

        // A client that does not say which seat is whose still has to work.
        if (!hostSeatSeen) {
            request.seats.firstOrNull { !it.isAi }?.let { lobby.hostSeat = it }
        }

        // A member the host has not placed anywhere loses whatever seat they had, so they are
        // given the first slot still free instead of keeping one the host emptied.
        val placed = request.seats.mapNotNull { seat ->
            seat.clientId.takeIf { it.isNotBlank() }
        }.toSet()
        lobby.seatsByClient.keys.removeAll { it !in placed }

        lobby.bots.clear()
        request.seats.filter { it.isAi }.take(lobby.maxPlayers - 1).forEach {
            lobby.bots.add(BotSeat(it.botDifficulty, it.slot, it.playerId, it.name, it.species))
        }

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
        if (lobby.members.size + lobby.botCount >= lobby.maxPlayers) {
            sendError(connection, "", "Lobby is full")
            return
        }
        if (lobby.inGame) {
            sendError(connection, "", "That lobby is already playing")
            return
        }

        leaveCurrentMatch(connection, "'${connection.name}' went back to a lobby")

        // Keep the species they arrived with, and give them one if they arrived with none: a
        // blank species has nothing to send, and every client would roll a new one per update.
        connection.species = request.species
            .ifBlank { connection.species }
            .ifBlank { Species.names.random() }

        // A joining client has to actually join the member list, or every broadcast of the lobby
        // state skips them and they sit in a room the server cannot see.
        if (lobby.members.none { it.clientId == connection.clientId }) {
            lobby.members.add(connection)
        }

        connection.lobbyId = lobby.id
        broadcastLobby(lobby)
        broadcastLobbyList()
    }

    private fun onLeaveLobby(connection: ClientConnection) {
        removeFromLobby(connection)
    }

    /**
     * A player taking a different seat. The host owns the arrangement, but anybody may move: the
     * seat they want swaps with them, a bot included, so nobody is ever dropped and nothing is
     * clamped back. Ids do not change, so a player keeps their colour.
     */
    private fun onMoveSeat(connection: ClientConnection, request: MoveSeat) {
        val lobby = lobbies[connection.lobbyId] ?: return
        if (lobby.inGame) return
        if (request.slot !in 0 until SLOT_COUNT) return

        lobby.moveTo(connection, request.slot)

        GameLog.info(
            "ServerCore",
            "'${connection.name}' took seat ${request.slot + 1} in '${lobby.name}'"
        )

        broadcastLobby(lobby)
        broadcastLobbyList()
    }

    /**
     * Takes a connection out of its lobby.
     *
     * A lobby that is already playing keeps existing, so the browser can still show it, and it is
     * dropped together with its match. A lobby nobody is left in is dropped here. The connection's
     * lobby id is cleared either way, or the next thing they do says they are still in a lobby.
     */
    private fun removeFromLobby(connection: ClientConnection) {
        val id = connection.lobbyId ?: return
        connection.lobbyId = null
        connection.ready = false

        val lobby = lobbies[id] ?: return
        lobby.members.removeIf { it.clientId == connection.clientId }

        // The seat they were arranged into goes with them, or a later joiner would inherit it.
        lobby.seatsByClient.remove(connection.clientId)

        if (lobby.inGame) {
            // The players are in the match now, so this lobby is only a name and a player count.
            broadcastLobbyList()
            return
        }

        if (lobby.members.isEmpty()) {
            lobbies.remove(id)
        } else {
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

        startMatch(
            members = lobby.members.map { it },
            mapId = lobby.mapId,
            fogOfWar = lobby.fogOfWar,
            gameMode = lobby.gameMode,
            host = lobby.host,
            botSeats = lobby.bots.toList(),
            hostSlot = lobby.hostSeat.slot,
            hostPlayerId = lobby.hostSeat.playerId,
            hostMapJson = lobby.hostMapJson,
            // A lobby game has to fill every place the map offers. A queue match cannot: its map
            // comes from the server's pool and its size is not the host's to arrange.
            requireAllPlaces = true,
            // The seating the lobby worked out, so the match starts with the same arrangement
            // everybody was looking at, ids included.
            memberSlots = lobby.seatAssignments()
                .filterIsInstance<Seat.Human>()
                .associate { it.connection.clientId to it.slot },
            memberIds = lobby.seatAssignments()
                .filterIsInstance<Seat.Human>()
                .associate { it.connection.clientId to it.playerId },
        )

        // Kept while the match runs, so the browser can show it as in game. Dropped with the
        // match. The players' lobby ids are cleared, or queueing later says they are still in one.
        val matchId = connection.matchId
        if (matchId.isNullOrBlank()) {
            // The match did not start after all (an empty place, or a map that could not be read),
            // so the lobby stays as it was and everybody stays in it.
            return
        }

        lobby.inGame = true
        lobby.matchId = matchId
        lobby.members.forEach { it.lobbyId = null }
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
        // Every lobby the server holds, including the closed and in game ones, because the
        // browser shows what is going on and only offers to join the open ones.
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
        if (!connection.matchId.isNullOrBlank() || !connection.lobbyId.isNullOrBlank()) {
            sendError(connection, "", "Leave your current lobby or match first")
            return
        }

        queue.addLast(connection)
        broadcastQueueStatus()

        // Debug mode: the first client to queue faces bots instead of waiting for a second
        // human, so the server side bot path can be checked with one client.
        if (botMatchOnFirstQueueJoin > 0 && queue.size == 1) {
            val solo = queue.removeFirst()
            startMatch(
                members = listOf(solo),
                mapId = config.mapPool.random(),
                fogOfWar = true,
                gameMode = GameMode.CRYSTALS.name,
                botSeats = List(botMatchOnFirstQueueJoin) { index ->
                    BotSeat(botMatchDifficulty, slot = index + 1, playerId = index + 2, name = "Bot ${index + 2}")
                },
            )
            broadcastQueueStatus()
            return
        }

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

    private fun startMatch(
        members: List<ClientConnection>,
        mapId: String,
        fogOfWar: Boolean,
        gameMode: String,
        /** The host, whose arrangement the seating is built around. */
        host: ClientConnection = members.first(),
        /** The host's arrangement: [LobbySeat] per bot. */
        botSeats: List<BotSeat> = emptyList(),
        hostSlot: Int = 0,
        hostPlayerId: Int = 1,
        /** The host's own map, when it is not on the server. */
        hostMapJson: String? = null,
        /**
         * Whether the match must fill every place the map offers. True for a lobby, whose host
         * arranged the seats, and false for a queue match, whose map comes from the server's pool.
         */
        requireAllPlaces: Boolean = false,
        /** Where the lobby put each human, by client id. Empty for a queue match. */
        memberSlots: Map<String, Int> = emptyMap(),
        /** The lobby id each human keeps, by client id. Empty for a queue match. */
        memberIds: Map<String, Int> = emptyMap(),
    ) {
        val map: MapConverted = if (hostMapJson != null) {
            // A map the host owns, sent to us and passed on to every client in match_found, so
            // nobody has to have this file locally.
            try {
                createMapFromJson(hostMapJson)
            } catch (e: Exception) {
                GameLog.error("ServerCore", "Host sent a map that could not be read", e)
                members.forEach { sendError(it, "", "The host's map could not be read") }
                return
            }
        } else {
            try {
                loadMap(mapId)
            } catch (e: Exception) {
                members.forEach { sendError(it, "", "Map '$mapId' not found") }
                return
            }
        }

        val players = ArrayList<Player>()

        // Ids are the lobby's, so a player keeps their colour wherever they sit. The slot only
        // decides which map place they own, and a slot past the map's player places means no
        // place at all, which is how spectating works.
        val playersBySlot = HashMap<Int, Player>()
        val taken = HashSet<Int>()
        val usedIds = HashSet<Int>()

        fun freeSlot(preferred: Int?): Int {
            if (preferred != null && preferred in 0 until 8 && taken.add(preferred)) return preferred
            val fallback = (0 until 8).firstOrNull { it !in taken } ?: return -1
            taken.add(fallback)
            return fallback
        }

        /** Keeps the lobby's id when it is still free, otherwise takes the lowest unused one. */
        fun seatId(preferred: Int): Int {
            if (preferred > 0 && usedIds.add(preferred)) return preferred
            val fallback = (1..8).firstOrNull { usedIds.add(it) } ?: return -1
            return fallback
        }

        members.filter { it === host }.forEach { connection ->
            val slot = freeSlot(hostSlot)
            if (slot < 0) return@forEach
            val id = seatId(hostPlayerId)
            if (id < 0) return@forEach
            val player = Player(name = connection.name, id = id, teamId = id)
            player.clientId = connection.clientId
            playersBySlot[slot] = player
        }

        members.filter { it !== host }.forEach { connection ->
            // The lobby's own arrangement, so a joiner who took a seat keeps it when the match
            // starts instead of being pushed back into the first free slot.
            val slot = freeSlot(memberSlots[connection.clientId])
            if (slot < 0) return@forEach
            val id = seatId(memberIds[connection.clientId] ?: -1)
            if (id < 0) return@forEach
            val player = Player(name = connection.name, id = id, teamId = id)
            player.clientId = connection.clientId
            playersBySlot[slot] = player
        }

        fun difficultyOf(name: String) = when (name.lowercase()) {
            "easy" -> Difficulty.EASY
            "hard" -> Difficulty.HARD
            "insane" -> Difficulty.INSANE
            else -> Difficulty.NORMAL
        }

        val bots = ArrayList<Pair<Player, Difficulty>>()
        botSeats.forEach { botSeat ->
            val slot = freeSlot(botSeat.slot)
            if (slot < 0) return@forEach
            val id = seatId(botSeat.playerId)
            if (id < 0) return@forEach
            val bot = Player(
                name = botSeat.name.ifBlank { "Bot $id" },
                id = id,
                teamId = id,
            )
            // The host picked this species in the slot dialog, so honour it. Only fall back to a
            // random one if nothing was sent.
            bot.species = botSeat.species.ifBlank { Species.names.random() }
            playersBySlot[slot] = bot
            bots.add(bot to difficultyOf(botSeat.difficulty))
        }

        // Map places follow the slots, not the ids.
        val playerIdBySlot = playersBySlot.entries.associate { (slot, player) -> slot to player.id }
        val botDifficultyById = bots.associate { (bot, difficulty) -> bot.id to difficulty.name.lowercase() }

        // A lobby game fills every place the map offers. Starting with an empty place would leave
        // a base nobody owns, free for anyone to walk into. The host's screen blocks it too, and
        // the server insists so the rule is real.
        if (requireAllPlaces) {
            val unfilled = map.labels
                .filter { it.labelName == "player" }
                .mapNotNull { it.data["id"]?.toIntOrNull() }
                .filter { playersBySlot[it] == null }

            if (unfilled.isNotEmpty()) {
                members.forEach { sendError(it, "", "Every player place on the map needs somebody") }
                return
            }
        }
        val matchId = UUID.randomUUID().toString().take(8)

        for (slot in playersBySlot.keys.sorted()) playersBySlot[slot]?.let { players.add(it) }

        GameLog.info(
            "ServerCore",
            SeatingLog.describe(
                "seating before match $matchId",
                (botSeats.map {
                    SeatingLog.row(it.slot, it.playerId, it.name, isAi = true, difficulty = it.difficulty)
                } + SeatingLog.row(hostSlot, hostPlayerId, host.name, isAi = false)),
            )
        )

        val battle = BattleSetup.build(
            map = map,
            players = players,
            gameEndCondition = CaptureBases(),
            gameMode = try { GameMode.valueOf(gameMode) } catch (e: Exception) { GameMode.CRYSTALS },
            season = Season.SUMMER,
            turnTimeLimitSeconds = config.turnTimeLimitSeconds,
            playerIdBySlot = playerIdBySlot,
        )

        // Whoever ended up past the map's player places owns no base and only watches. Marked
        // once, here, so the turn manager skips them on both sides and every message can say so.
        players.forEach { player ->
            player.isSpectator = battle.field.getCapturablesOf(player).none { it.isBase }
        }

        GameLog.info(
            "ServerCore",
            SeatingLog.describe(
                "match $matchId seating with map places",
                players.map { player ->
                    SeatingLog.row(
                        slot = playerIdBySlot.entries.firstOrNull { it.value == player.id }?.key ?: -1,
                        playerId = player.id,
                        name = player.name,
                        isAi = player.clientId == null,
                        difficulty = botDifficultyById[player.id] ?: "normal",
                    )
                },
                mapPlaces = map.labels
                    .mapNotNull { label ->
                        val slotId = label.data["id"]?.toIntOrNull() ?: return@mapNotNull null
                        val owner = label.data["player_id"]?.toIntOrNull() ?: return@mapNotNull null
                        slotId to owner
                    }.toMap(),
            )
        )
        // Not clientControlled: this battle is the authority, so it must run its own bots.
        bots.forEach { (bot, botDifficulty) ->
            battle.botManager.add(BotFactory.create(bot, battle, botDifficulty))
        }

        // A map sent by a host already carries `player_id` on its player labels, written by the
        // host's setup screen from its own temporary player ids. Trusting those would decide map
        // ownership by whatever the client happened to number, so they are dropped and the host's
        // seat arrangement decides who owns which place instead.
        clearMapOwnership(map)

        val match = Match(matchId, battle, fogOfWar)

        // [players] is in slot order, so a member's player must be looked up by the connection
        // and never by its position in the member list.
        val playerOfConnection = HashMap<ClientConnection, Player>()
        members.forEach { connection ->
            val player = players.firstOrNull { it.clientId == connection.clientId } ?: return@forEach
            connection.matchId = match.id
            connection.playerId = player.id
            match.clientIds[player.id] = connection.clientId
            playerOfConnection[connection] = player
        }
        matches[match.id] = match

        val mapJson = map.mapSave.toGson()

        members.forEach { connection ->
            val you = playerOfConnection[connection] ?: return@forEach

            val found = MatchFound()
            found.matchId = match.id
            found.you = you.id
            found.mapId = mapId
            found.mapJson = mapJson
            found.gameMode = battle.gameMode.name
            found.season = battle.season.name
            found.daytime = "DAY"
            found.fogOfWar = fogOfWar
            found.turnTimeLimitSeconds = battle.turnTimeLimitSeconds

            // Who owns which map place. The client must not guess this from the map's own
            // labels, or it hands the first place to player one and gives a spectator a base.
            found.mapOwnerBySlot.putAll(playerIdBySlot)

            // Every seated player, humans and bots alike. The client builds its mirror from this
            // list, including the turn order, so it must match the battle exactly: slot order
            // for the order, id for the colour.
            players.forEach { player ->
                found.players.add(LobbyMember().also {
                    // A bot has no client id, which is also how the client tells them apart.
                    it.clientId = player.clientId ?: ""
                    it.name = player.name
                    it.playerId = player.id
                    it.slot = playerIdBySlot.entries.firstOrNull { entry -> entry.value == player.id }?.key ?: -1
                    it.isAi = player.clientId == null
                    it.botDifficulty = botDifficultyById[player.id] ?: "normal"
                    it.species = player.species
                    it.isSpectator = player.isSpectator
                })
            }

            GameLog.verbose(
                "ServerCore",
                "match_found for ${connection.name}: " +
                        SeatingLog.describe("sent", found.players.toList()) +
                        " you=p${you.id} match=${match.id}"
            )

            sendTo(connection, Msg.MATCH_FOUND, "", found)
            match.sendSnapshot(connection)
        }

        GameLog.info(
            "ServerCore",
            "Match ${match.id} started on '$mapId' with ${members.size} human(s) and ${bots.size} bot(s)"
        )
    }

    /**
     * Drops any owner baked into a map's player labels, so the seat arrangement decides who owns
     * which place. Players are then numbered by slot, and `Battle.setupDefaultPlayerIds` gives
     * the first map place to the first slot.
     */
    private fun clearMapOwnership(map: MapConverted) {
        map.labels
            .filter { it.labelName == "player" }
            .forEach { it.data.removeKey("player_id") }
    }

    private fun onMatchCommand(connection: ClientConnection, requestId: String, request: MatchCommand) {        val match = matches[connection.matchId] ?: return
        match.onCommand(connection, requestId, request)
    }

    private fun onSnapshotRequest(connection: ClientConnection) {
        // Info, not verbose: this pairs with the client's resync line, and if they ever disagree
        // about what happened the two lines side by side are the whole story.
        GameLog.info(
            "ServerCore",
            "Match ${connection.matchId}: ${connection.name} asked for a snapshot " +
                    "(state ${matches[connection.matchId]?.battle?.gameStateId})"
        )
        matches[connection.matchId]?.sendSnapshot(connection)
    }

    /**
     * The player quit the match on purpose, by going back to the menu. The connection is kept
     * because they are still connected to the server and may queue again, but it is no longer a
     * member of this match, so the match stops broadcasting to it and stops once it is empty.
     */
    private fun onLeaveMatch(connection: ClientConnection) {
        leaveCurrentMatch(connection, "'${connection.name}' left the match")
    }

    /**
     * Takes a connection out of whatever match it is in.
     *
     * A connection belongs to at most one match. Without this, a player who went back to the menu
     * and started another game stayed a member of the first one: the old match kept playing and
     * broadcasting, so one client was handed two command streams at once and every message looked
     * like a gap. Called whenever a connection moves on to a lobby or the queue.
     */
    private fun leaveCurrentMatch(connection: ClientConnection, reason: String) {
        val match = matches[connection.matchId] ?: run {
            connection.matchId = null
            return
        }

        GameLog.info("ServerCore", "Match ${match.id}: $reason")
        match.removeMember(connection)
        connection.matchId = null
    }

    private inner class Match(val id: String, val battle: ServerBattle, val fogOfWar: Boolean) {

        val clientIds = HashMap<Int, String>()
        var lastCommandTime = System.nanoTime()

        private var deadlinePlayerId = -1
        private var deadlineAt = 0L

        val isFinished: Boolean get() = battle.isGameOver

        /** Set by [stop]. A stopped match is dropped from the table on the next update. */
        var finished = false

        private var stopped = false

        /**
         * True once every member has gone. A match of bots with no one watching has nothing left
         * to decide, so it is stopped rather than played out to an empty room.
         */
        fun hasNoMembersLeft(): Boolean = connectionFor().isEmpty() && !stopped

        init {
            battle.commandManager.onCommand = { command ->
                battle.gameStateId++
                lastCommandTime = System.nanoTime()

                // Every command passes through here, whether it came from a client, from a bot or
                // from the turn timer, so this is the one place that tells the clients about it.
                // Broadcasting from the human path alone meant bot turns were played on the server
                // and never reached anybody watching.
                //
                // The stream is ordered and reliable, so a client that applies each command stays
                // in step on its own. A full snapshot is only sent when a client really needs one:
                // a rejection, a state gap, or a reconnect.
                val broadcast = MatchCommandBroadcast()
                broadcast.matchId = id
                broadcast.stateId = battle.gameStateId
                broadcast.playerId = command.playerId
                broadcast.commandName = command.javaClass.name
                broadcast.commandJson = command.toGson()

                connectionFor().forEach { sendTo(it, Msg.MATCH_COMMAND_BROADCAST, "", broadcast) }
            }
        }

        fun tick(dt: Float) {
            battle.update(dt)
            checkTurnTimeout()

            if (battle.isGameOver) {
                val ended = MatchEnded()
                ended.matchId = id
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
            endTurn.playerId = current

            // Through the manager, so a forced end of turn is recorded and broadcast like any
            // other command.
            if (!battle.commandManager.execute(endTurn)) return

            GameLog.info("ServerCore", "Match $id: player $current ran out of time")
        }

        fun onCommand(connection: ClientConnection, requestId: String, request: MatchCommand) {
            val expectedPlayerId = connection.playerId
            if (expectedPlayerId <= 0) {
                sendError(connection, requestId, "You are not in this match")
                return
            }

            // Whose turn it is, asked before the state counter. A player who is not up, which
            // includes somebody who came along to watch, was being told their state was out of
            // date, which is true but says nothing about the real problem.
            val current = battle.turnManager.currentPlayer.id
            if (expectedPlayerId != current) {
                val player = battle.turnManager.getPlayerById(expectedPlayerId)
                val watching = player != null &&
                        (player.isSpectator ||
                                ctmn.petals.sim.FogOfWar.isSpectator(battle.field, player))

                reject(
                    connection, requestId,
                    if (watching) "You are watching this match, not playing it" else "It is not your turn"
                )
                return
            }

            if (request.expectedStateId != 0 && request.expectedStateId != battle.gameStateId) {
                reject(connection, requestId, "State out of date")
                return
            }

            // A move the client would not decide on its own: resolve it against the full state
            // and play the real result instead. The sender never applied anything, so the
            // broadcast is just the next command in everyone's stream.
            if (request.commandName == ctmn.petals.playscreen.commands.TryMoveCommand::class.java.name) {
                onTryMove(connection, requestId, request)
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

            // Run it through the command manager, not straight through the command: the manager is
            // where the rolls a command draws are recorded onto it, so every client can replay the
            // server's numbers. It also broadcasts the accepted command, from one place, for
            // clients and bots alike.
            val ok = battle.commandManager.execute(command)
            if (!ok) {
                reject(connection, requestId, "Command failed")
                return
            }

            GameLog.info(
                "ServerCore",
                "Match $id: ${connection.name} -> ${command.javaClass.simpleName} (state ${battle.gameStateId})"
            )

            // No snapshot here on purpose, and no broadcast either: the command listener does
            // both, so a client's own command and a bot's command travel the same path. Sending
            // one snapshot per command made every single action cost a full state transfer.
        }

        /**
         * Resolves a try move against the full state and plays the real move.
         *
         * The destination the client asked for may hold a hidden unit or lie past one, which the
         * client could not see. Then the unit stops at the furthest tile it can actually reach,
         * with the reason, and every client animates that same result.
         */
        private fun onTryMove(connection: ClientConnection, requestId: String, request: MatchCommand) {
            val expectedPlayerId = connection.playerId
            val ask = try {
                fromGson(request.commandJson, ctmn.petals.playscreen.commands.TryMoveCommand::class.java)
            } catch (e: Exception) {
                reject(connection, requestId, "Bad command payload")
                return
            }

            val unit = battle.field.findUnit(ask.unitId)
            if (unit == null || unit.playerId != expectedPlayerId) {
                reject(connection, requestId, "Not your unit")
                return
            }

            if (!unit.canMove()) {
                reject(connection, requestId, "Unit cannot move")
                return
            }

            val route = battle.field.findMovePath(unit, ask.tileX, ask.tileY)
            val destFree = battle.field.getTile(ask.tileX, ask.tileY)?.isPassableAndFree() == true

            val move = if (route != null && destFree) {
                ctmn.petals.playscreen.commands.MoveUnitCommand(ask.unitId, ask.tileX, ask.tileY)
            } else {
                // Stopped short: the way is held or the budget runs out first, and the two must
                // not share one label. A held destination is a genuine trap; an empty one the
                // client simply could not reach is not.
                val stop = furthestReachable(unit, ask.tileX, ask.tileY)
                if (stop == null) {
                    reject(connection, requestId, "Unit cannot move")
                    return
                }

                val held = battle.field.getUnit(ask.tileX, ask.tileY)
                    ?.let { !it.isAlly(unit) } == true

                ctmn.petals.playscreen.commands.MoveUnitCommand(ask.unitId, stop.first, stop.second).also {
                    it.reason = if (held || !destFree) ctmn.petals.playscreen.commands.MoveUnitCommand.REASON_BLOCKED
                    else ctmn.petals.playscreen.commands.MoveUnitCommand.REASON_SHORT
                }
            }

            move.playerId = expectedPlayerId
            // Through the manager, so the command is recorded, broadcast and counted exactly like
            // every other one.
            if (!battle.commandManager.execute(move)) {
                reject(connection, requestId, "Command failed")
                return
            }

            GameLog.info(
                "ServerCore",
                "Match $id: ${connection.name} try-move ${ask.unitId} to ${ask.tileX},${ask.tileY} " +
                        "resolved to ${move.tileX},${move.tileY}" +
                        (if (move.reason.isNotBlank()) " (${move.reason})" else "") +
                        " (state ${battle.gameStateId})"
            )
        }

        /**
         * The reachable tile closest to [destX]:[destY], or null when the unit is boxed in.
         * One search answers for the whole map. The server sees every unit, so this is the
         * honest answer to "how far could it go".
         */
        private fun furthestReachable(
            unit: ctmn.petals.unit.UnitActor,
            destX: Int,
            destY: Int,
        ): Pair<Int, Int>? {
            var best: Pair<Int, Int>? = null
            var bestDst = Int.MAX_VALUE

            for (tile in battle.field.reachableMoveTiles(unit)) {
                if (tile.first == unit.tiledX && tile.second == unit.tiledY) continue
                if (battle.field.getTile(tile.first, tile.second)?.isPassableAndFree() != true) continue

                val dst = ctmn.petals.playstage.tiledDst(tile.first, tile.second, destX, destY)
                if (dst < bestDst) {
                    bestDst = dst
                    best = tile
                }
            }

            return best
        }

        private fun reject(connection: ClientConnection, requestId: String, reason: String) {
            GameLog.info("ServerCore", "Match $id: rejected '${connection.name}': $reason")

            sendTo(connection, Msg.COMMAND_REJECTED, requestId, CommandRejected().also {
                it.requestId = requestId
                it.reason = reason
            })
            sendSnapshot(connection)
        }

        /** Drops a member without ending the match, used when a player quits to the menu. */
        fun removeMember(connection: ClientConnection) {
            val entry = clientIds.entries.firstOrNull { it.value == connection.clientId } ?: return
            clientIds.remove(entry.key)
            GameLog.verbose(
                "ServerCore",
                "Match $id: player ${entry.key} (${connection.name}) is no longer a member"
            )
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

                // A spectator owns no tile, so they have no vision sources and fog would hide the
                // whole map from them. They get the unfiltered state instead, which is what makes
                // spectating work at all.
                val spectating = viewer != null &&
                        ctmn.petals.sim.FogOfWar.isSpectator(battle.field, viewer)

                if (viewer != null && !spectating) {
                    val visibility = ctmn.petals.sim.FogOfWar.compute(
                        battle.field, battle.turnManager.players, viewer
                    )
                    SnapshotFilter.apply(snapshot, visibility, viewer.teamId)
                } else if (spectating) {
                    GameLog.verbose("ServerCore", "sending the unfiltered map to spectator ${connection.name}")
                }
            }

            val message = MatchSnapshot()
            message.matchId = id
            message.stateId = battle.gameStateId
            message.snapshotJson = snapshot.toGson()
            sendTo(connection, Msg.MATCH_SNAPSHOT, "", message)
        }

        private fun connectionFor(): List<ClientConnection> =
            clientIds.values.mapNotNull { clientsById[it] }

        /**
         * Ends the match and forgets its members. A match nobody is left in must not keep playing:
         * it would burn a turn every second and keep broadcasting to connections that have already
         * moved on to another match, which is how two command streams end up interleaved on one
         * client.
         */
        fun stop(reason: String) {
            if (stopped) return
            stopped = true

            GameLog.info(
                "ServerCore",
                "Match $id stopped ($reason) with ${connectionFor().size} connection(s) left"
            )

            clientIds.clear()
            finished = true
        }
    }

    private inner class Lobby(
        val id: String,
        /** Mutable because the host keeps editing after the lobby exists, and every change syncs. */
        var name: String,
        var mapId: String,
        /** The map's display name. A map id can be a file name or a UUID, so it is not shown. */
        var mapName: String,
        var host: ClientConnection,
        val maxPlayers: Int,
        var fogOfWar: Boolean,
        var gameMode: String,
        var season: String = "SUMMER",
        var daytime: String = "DAY",
        /**
         * Whether the host has published the lobby. It exists from the moment they open the setup
         * screen so the arrangement can be built privately, and it appears in the browser when
         * they press to open it.
         */
        var listed: Boolean = false,
        /** Set when the host picked a map that only exists on their machine. */
        var hostMapJson: String? = null,
        /** True once the match from this lobby started. The lobby is then only a browser entry. */
        var inGame: Boolean = false,
        /** The match this lobby started, so the browser entry goes when the match does. */
        var matchId: String? = null,
    ) {
        val members = ArrayList<ClientConnection>()

        /** Where the host sits and which id they keep, so their colour survives. */
        var hostSeat = LobbySeat().apply { slot = 0; playerId = 1 }

        /**
         * Where each other human sits, as the host arranged it, keyed by client id. Without this
         * the host's arrangement of people was thrown away and the server re-derived it, so a
         * seat the host swapped snapped back and a joiner could not be given a species.
         */
        val seatsByClient = HashMap<String, LobbySeat>()

        /** Bot seats, each with its difficulty, slot and lobby id. */
        val bots = ArrayList<BotSeat>()

        val botCount get() = bots.size

        init {
            members.add(host)
        }

        /** The seat a connection holds, arranged or not. */
        private fun arrangedSeat(connection: ClientConnection): LobbySeat {
            if (connection === host) return hostSeat

            return seatsByClient.getOrPut(connection.clientId) {
                val current = seatAssignments().firstOrNull {
                    it is Seat.Human && it.connection === connection
                }

                LobbySeat().apply {
                    slot = current?.slot ?: 0
                    playerId = current?.playerId ?: nextFreePlayerId(seatAssignments())
                    species = connection.species
                }
            }
        }

        /** The slot a connection is in right now. */
        fun slotOf(connection: ClientConnection): Int =
            seatAssignments().firstOrNull { it is Seat.Human && it.connection === connection }?.slot
                ?: -1

        /**
         * Moves a connection to [slot]. Whoever is in the way swaps with them, so a bot goes to
         * the seat the player left and nobody is ever dropped. Ids do not change, so everybody
         * keeps their colour.
         */
        fun moveTo(connection: ClientConnection, slot: Int) {
            val from = slotOf(connection)
            if (from < 0 || from == slot || slot !in 0 until maxPlayers) return

            val bot = bots.firstOrNull { it.slot == slot }
            val other = members.firstOrNull { it !== connection && slotOf(it) == slot }

            when {
                bot != null -> bot.slot = from
                other != null -> arrangedSeat(other).slot = from
            }

            arrangedSeat(connection).slot = slot
        }

        fun toInfo(): LobbyInfo = LobbyInfo().also {
            it.id = id
            it.name = name
            it.mapId = mapId
            it.mapName = mapName
            it.hostName = host.name
            it.players = members.size + botCount
            it.maxPlayers = maxPlayers
            it.state = if (inGame) "PLAYING" else "WAITING"
            it.listed = listed
        }

        fun toState(): LobbyStateMsg = LobbyStateMsg().also { state ->
            state.id = id
            state.name = name
            state.hostClientId = host.clientId
            state.mapId = mapId
            state.mapName = mapName
            state.gameMode = gameMode
            state.fogOfWar = fogOfWar
            state.season = season
            state.daytime = daytime
            state.maxPlayers = maxPlayers
            state.mapJson = hostMapJson ?: ""
            state.state = if (members.all { it.ready || it === host }) "STARTING" else "WAITING"

            // One entry per slot, empty ones included, so a client can place each member in the
            // slot it was actually given.
            val bySlot = seatAssignments().associateBy { it.slot }
            for (slot in 0 until 8) {
                when (val seat = bySlot[slot]) {
                    null -> state.members.add(LobbyMember().also { it.playerId = -1 })

                    is Seat.Bot -> state.members.add(LobbyMember().also {
                        it.slot = slot
                        it.name = seat.name.ifBlank { "Bot ${slot + 1}" }
                        it.playerId = seat.playerId
                        it.isAi = true
                        it.botDifficulty = seat.difficulty
                        it.species = seat.species
                        it.ready = true
                    })

                    is Seat.Human -> state.members.add(LobbyMember().also {
                        val arranged =
                            if (seat.connection === host) hostSeat
                            else seatsByClient[seat.connection.clientId]

                        it.slot = slot
                        it.clientId = seat.connection.clientId
                        it.name = seat.connection.name
                        it.playerId = seat.playerId
                        it.ready = seat.connection.ready || seat.connection === host
                        it.host = seat.connection === host

                        // The species the host picked for this seat wins, so the host can change
                        // anybody's. Otherwise it is what they arrived with.
                        it.species = arranged?.species?.takeIf { species -> species.isNotBlank() }
                            ?: seat.connection.species
                    })
                }
            }

            GameLog.verbose(
                "ServerCore",
                "broadcasting lobby '${state.name}': " + SeatingLog.describe("seats", state.members.toList())
            )
        }

        /**
         * Who sits where. The host's arrangement wins: their own seat, the bots' seats and every
         * other human's seat are honoured, and only somebody the host has not placed takes the
         * first slot still free. A slot past the map's player places has no base, which is how
         * spectating works.
         */
        fun seatAssignments(): List<Seat> {
            val seats = ArrayList<Seat>(members.size + bots.size)
            val taken = HashSet<Int>()

            seats.add(Seat.Human(host, hostSeat.slot, hostSeat.playerId))
            taken.add(hostSeat.slot)

            bots.forEach { bot ->
                if (taken.add(bot.slot)) {
                    seats.add(Seat.Bot(bot.difficulty, bot.slot, bot.playerId, bot.name, bot.species))
                }
            }

            members.filter { it !== host }.forEach { connection ->
                val arranged = seatsByClient[connection.clientId]

                val slot = arranged?.slot?.takeIf { it in 0 until maxPlayers && taken.add(it) }
                    ?: (0 until maxPlayers).firstOrNull { taken.add(it) }
                    ?: return@forEach

                // The id the host arranged for them, when it is still free, so a swap keeps
                // everybody's colour.
                val id = arranged?.playerId?.takeIf { it > 0 && seats.none { seat -> seat.playerId == it } }
                    ?: nextFreePlayerId(seats)

                seats.add(Seat.Human(connection, slot, id))
            }

            return seats.sortedBy { it.slot }
        }

        private fun nextFreePlayerId(seats: List<Seat>): Int {
            val used = seats.mapNotNull { it.playerId.takeIf { id -> id > 0 } }.toHashSet()
            return (1..maxPlayers).firstOrNull { it !in used } ?: (maxPlayers + 1)
        }
    }

    /** A bot seat in a lobby, with the slot and lobby id the host gave it. */
    private data class BotSeat(
        val difficulty: String,
        /** Mutable: a player taking the bot's seat swaps it to the seat they left. */
        var slot: Int,
        val playerId: Int,
        val name: String,
        val species: String = "",
    )

    /** Who sits in which slot, with which lobby id, before players exist. */
    private sealed interface Seat {
        val slot: Int
        val playerId: Int

        data class Human(
            val connection: ClientConnection,
            override val slot: Int,
            override val playerId: Int,
        ) : Seat

        data class Bot(
            val difficulty: String,
            override val slot: Int,
            override val playerId: Int,
            val name: String,
            val species: String = "",
        ) : Seat
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
