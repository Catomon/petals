package ctmn.petals.net

import ctmn.petals.net.proto.*
import ctmn.petals.utils.fromGson
import ctmn.petals.utils.toGson
import io.netty.bootstrap.Bootstrap
import io.netty.channel.*
import io.netty.channel.nio.NioEventLoopGroup
import io.netty.channel.socket.SocketChannel
import io.netty.channel.socket.nio.NioSocketChannel
import io.netty.handler.codec.LengthFieldBasedFrameDecoder
import io.netty.handler.codec.LengthFieldPrepender
import io.netty.handler.codec.string.StringDecoder
import io.netty.handler.codec.string.StringEncoder
import java.nio.charset.StandardCharsets
import java.util.UUID

/**
 * Client side of the dedicated server protocol. Used by the game's online screens and by
 * headless tests. Callbacks are invoked on the netty IO thread; UI code must hand off to the
 * render thread (the game does this with `Gdx.app.postRunnable` / scene2d actions).
 */
class ServerClient {

    interface Listener {
        fun onWelcome(welcome: Welcome) {}
        fun onLobbyList(list: LobbyList) {}
        fun onLobbyState(state: LobbyStateMsg) {}
        fun onQueueStatus(status: QueueStatus) {}
        fun onMatchFound(found: MatchFound) {}
        fun onMatchCommand(broadcast: MatchCommandBroadcast) {}
        fun onMatchSnapshot(snapshot: MatchSnapshot) {}
        fun onCommandRejected(rejected: CommandRejected) {}
        fun onMatchEnded(ended: MatchEnded) {}
        fun onError(error: ErrorMsg) {}
        fun onConnectFailed(cause: Throwable?) {}
        fun onDisconnected() {}
    }

    var listener: Listener? = null

    var clientId: String = UUID.randomUUID().toString()
        private set
    var displayName: String = "Player"
        private set

    private var group: EventLoopGroup? = null
    private var channel: Channel? = null

    @Volatile
    var connected = false
        private set

    val isConnected: Boolean get() = connected && channel?.isActive == true

    /**
     * Starts connecting. This never blocks, so it is safe to call from the render thread:
     * [Listener.onConnectFailed] reports a failure and [Listener.onWelcome] reports success.
     */
    fun connect(host: String, port: Int, clientId: String, name: String) {
        // A reconnect must not leave the previous event loop group behind: its threads are
        // not daemons and would keep the process alive forever.
        close()

        this.clientId = clientId
        this.displayName = name

        val newGroup = NioEventLoopGroup(1)
        group = newGroup

        val bootstrap = Bootstrap()
            .group(newGroup)
            .channel(NioSocketChannel::class.java)
            .option(ChannelOption.SO_KEEPALIVE, true)
            .handler(object : ChannelInitializer<SocketChannel>() {
                override fun initChannel(ch: SocketChannel) {
                    ch.pipeline()
                        .addLast(LengthFieldBasedFrameDecoder(1 shl 20, 0, 4, 0, 4))
                        .addLast(LengthFieldPrepender(4))
                        .addLast(StringDecoder(StandardCharsets.UTF_8))
                        .addLast(StringEncoder(StandardCharsets.UTF_8))
                        .addLast(Handler())
                }
            })

        // The handshake is sent from the handler once the channel is really active, so it
        // can never be written into a socket that is still being set up.
        val future = bootstrap.connect(host, port)
        channel = future.channel()

        future.addListener { f ->
            if (!f.isSuccess) {
                connected = false
                listener?.onConnectFailed(f.cause())
            }
        }
    }

    fun close() {
        val currentChannel = channel
        channel = null
        connected = false
        currentChannel?.close()
        group?.shutdownGracefully()
        group = null
    }

    fun listLobbies() = send(Msg.LIST_LOBBIES, "", "")

    fun createLobby(request: CreateLobby) = send(Msg.CREATE_LOBBY, "", request)

    fun joinLobby(lobbyId: String, species: String = "") =
        send(Msg.JOIN_LOBBY, "", JoinLobby().also {
            it.lobbyId = lobbyId
            it.species = species
        })

    fun leaveLobby() = send(Msg.LEAVE_LOBBY, "", "")

    fun moveSeat(slot: Int) = send(Msg.MOVE_SEAT, "", MoveSeat().also { it.slot = slot })

    fun ready(ready: Boolean) = send(Msg.READY, "", Ready().also { it.ready = ready })

    fun startMatch() = send(Msg.START, "", "")

    fun queueJoin() = send(Msg.QUEUE_JOIN, "", "")

    fun queueLeave() = send(Msg.QUEUE_LEAVE, "", "")

    fun requestSnapshot() = send(Msg.SNAPSHOT_REQUEST, "", "")

    /** Tells the server we quit the match, so it stops sending this client its commands. */
    fun leaveMatch() = send(Msg.LEAVE_MATCH, "", "")

    fun ping() = send(Msg.PING, "", Ping().also { it.time = System.currentTimeMillis() })

    fun sendCommand(requestId: String, expectedStateId: Int, commandName: String, commandJson: String) =
        send(Msg.MATCH_COMMAND, requestId, MatchCommand().also {
            it.requestId = requestId
            it.expectedStateId = expectedStateId
            it.commandName = commandName
            it.commandJson = commandJson
        })

    private fun send(type: String, requestId: String, payload: Any) {
        val channel = channel ?: return
        channel.writeAndFlush(envelope(type, requestId, payload))
    }

    private fun envelope(type: String, requestId: String, payload: Any): String {
        val envelope = Envelope()
        envelope.type = type
        envelope.requestId = requestId
        envelope.payload = if (payload is String) payload else payload.toGson()
        return envelope.toGson()
    }

    private inner class Handler : SimpleChannelInboundHandler<String>() {

        override fun channelActive(ctx: ChannelHandlerContext) {
            connected = true
            super.channelActive(ctx)

            // Identify ourselves as soon as the connection is usable.
            val hello = Hello()
            hello.clientId = clientId
            hello.name = displayName
            ctx.writeAndFlush(envelope(Msg.HELLO, "", hello))
        }

        override fun channelInactive(ctx: ChannelHandlerContext) {
            connected = false
            listener?.onDisconnected()
            super.channelInactive(ctx)
        }

        override fun channelRead0(ctx: ChannelHandlerContext, msg: String) {
            val envelope = try {
                fromGson(msg, Envelope::class.java)
            } catch (e: Exception) {
                return
            }
            val listener = listener ?: return

            when (envelope.type) {
                Msg.WELCOME -> listener.onWelcome(fromGson(envelope.payload, Welcome::class.java))
                Msg.LOBBY_LIST -> listener.onLobbyList(fromGson(envelope.payload, LobbyList::class.java))
                Msg.LOBBY_STATE -> listener.onLobbyState(fromGson(envelope.payload, LobbyStateMsg::class.java))
                Msg.QUEUE_STATUS -> listener.onQueueStatus(fromGson(envelope.payload, QueueStatus::class.java))
                Msg.MATCH_FOUND -> listener.onMatchFound(fromGson(envelope.payload, MatchFound::class.java))
                Msg.MATCH_COMMAND_BROADCAST -> listener.onMatchCommand(fromGson(envelope.payload, MatchCommandBroadcast::class.java))
                Msg.MATCH_SNAPSHOT -> listener.onMatchSnapshot(fromGson(envelope.payload, MatchSnapshot::class.java))
                Msg.COMMAND_REJECTED -> listener.onCommandRejected(fromGson(envelope.payload, CommandRejected::class.java))
                Msg.MATCH_ENDED -> listener.onMatchEnded(fromGson(envelope.payload, MatchEnded::class.java))
                Msg.ERROR -> listener.onError(fromGson(envelope.payload, ErrorMsg::class.java))
                Msg.PONG -> {}
            }
        }
    }
}
