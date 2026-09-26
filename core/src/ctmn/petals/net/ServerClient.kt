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

    fun connect(host: String, port: Int, clientId: String, name: String) {
        this.clientId = clientId
        this.displayName = name

        group = NioEventLoopGroup(1)

        val bootstrap = Bootstrap()
            .group(group)
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

        val future = bootstrap.connect(host, port).sync()
        channel = future.channel()

        send(Msg.HELLO, "", Hello().also {
            it.clientId = clientId
            it.name = name
        })
    }

    fun close() {
        channel?.close()
        group?.shutdownGracefully()
        connected = false
    }

    fun listLobbies() = send(Msg.LIST_LOBBIES, "", "")

    fun createLobby(request: CreateLobby) = send(Msg.CREATE_LOBBY, "", request)

    fun joinLobby(lobbyId: String) = send(Msg.JOIN_LOBBY, "", JoinLobby().also { it.lobbyId = lobbyId })

    fun leaveLobby() = send(Msg.LEAVE_LOBBY, "", "")

    fun ready(ready: Boolean) = send(Msg.READY, "", Ready().also { it.ready = ready })

    fun startMatch() = send(Msg.START, "", "")

    fun queueJoin() = send(Msg.QUEUE_JOIN, "", "")

    fun queueLeave() = send(Msg.QUEUE_LEAVE, "", "")

    fun requestSnapshot() = send(Msg.SNAPSHOT_REQUEST, "", "")

    fun ping() = send(Msg.PING, "", Ping().also { it.time = System.currentTimeMillis() })

    fun sendCommand(requestId: String, expectedStateId: Int, commandName: String, commandJson: String) =
        send(Msg.MATCH_COMMAND, requestId, MatchCommand().also {
            it.requestId = requestId
            it.expectedStateId = expectedStateId
            it.commandName = commandName
            it.commandJson = commandJson
        })

    private fun send(type: String, requestId: String, payload: Any) {
        val envelope = Envelope()
        envelope.type = type
        envelope.requestId = requestId
        envelope.payload = if (payload is String) payload else payload.toGson()
        channel?.writeAndFlush(envelope.toGson())
    }

    private inner class Handler : SimpleChannelInboundHandler<String>() {

        override fun channelActive(ctx: ChannelHandlerContext) {
            connected = true
            super.channelActive(ctx)
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
