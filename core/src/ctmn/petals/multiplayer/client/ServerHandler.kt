package ctmn.petals.multiplayer.client

import ctmn.petals.multiplayer.JsonMessage
import ctmn.petals.multiplayer.json.serverres.ClientIdMessage
import ctmn.petals.multiplayer.toJsonMessage
import io.netty.channel.ChannelHandler
import io.netty.channel.ChannelInboundHandlerAdapter
import io.netty.channel.ChannelHandlerContext

@ChannelHandler.Sharable
class ServerHandler(private val clientManager: ClientManager) : ChannelInboundHandlerAdapter() {

    var ctx: ChannelHandlerContext? = null

    var serverListener: ServerListener? = null

    override fun channelUnregistered(ctx: ChannelHandlerContext) {
        super.channelUnregistered(ctx)
    }

    override fun channelActive(ctx: ChannelHandlerContext) {
        super.channelActive(ctx)

        this.ctx = ctx

        // when client is connected, send client id
        ctx.writeAndFlush(ClientIdMessage(clientManager.clientId).toJsonMessage())

        serverListener?.onConnected()
    }

    override fun channelInactive(ctx: ChannelHandlerContext?) {
        super.channelInactive(ctx)

        this.ctx = null

        serverListener?.onDisconnected()
    }

    override fun channelRegistered(ctx: ChannelHandlerContext?) {
        super.channelRegistered(ctx)
    }

    override fun channelRead(ctx: ChannelHandlerContext, msg: Any) {
        super.channelRead(ctx, msg)

        if (msg !is JsonMessage)
            return

        clientManager.receiveResponse(msg)
    }

    interface ServerListener {

        fun onIdentified(clientId: String)

        fun onConnected()

        fun onDisconnected()
    }
}
