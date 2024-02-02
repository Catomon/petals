package ctmn.petals.multiplayer.server

import com.google.gson.JsonObject
import ctmn.petals.multiplayer.JsonMessage
import ctmn.petals.multiplayer.json.serverres.ClientIdMessage
import ctmn.petals.multiplayer.json.serverres.Disconnected
import ctmn.petals.multiplayer.server.ClientsController.Companion.REASON_DISCONNECTED_BY_CLIENT
import ctmn.petals.multiplayer.server.ClientsController.Companion.REASON_KICKED
import ctmn.petals.multiplayer.server.ClientsController.Companion.REASON_KICKED_EXCEPTION
import ctmn.petals.multiplayer.toJsonMessage
import ctmn.petals.utils.fromGson
import io.netty.channel.ChannelFuture
import io.netty.channel.ChannelFutureListener
import io.netty.channel.ChannelHandlerContext
import io.netty.channel.ChannelInboundHandlerAdapter
import java.util.*

class ClientHandler(val clientsController: ClientsController) : ChannelInboundHandlerAdapter() {

    var clientId: String = "null"

    var isActive = false

    private var ctx: ChannelHandlerContext? = null

    private var disconnected = false

    fun disconnect(reason: String) {
        logger.info("Disconnecting client: $clientId with reason: $reason")
        sendMessage(Disconnected(reason).toJsonMessage())?.addListener(ChannelFutureListener.CLOSE)

        when (reason) {
            REASON_KICKED_EXCEPTION -> clientsController.clientsListener?.onClientDisconnected(this)
            REASON_KICKED -> clientsController.clientsListener?.onClientDisconnected(this)
            REASON_DISCONNECTED_BY_CLIENT -> clientsController.clientsListener?.onClientDisconnected(this)
        }

        disconnected = true
    }

    private fun receiveMessage(jsonMessage: JsonMessage) {
        val jsonObject = fromGson(jsonMessage.message, JsonObject::class.java)
        logger.info("Received message: ${jsonObject["id"].asString}")

        // client should send client_id first, so we can identify him, and if his id is "null"
        // we will generate a new one and send it back to him.
        // then the client will send its id again, and we will save it and call onClientIdentified()
        if (jsonMessage.id == "client_id") {
            val clientIdMessage = fromGson(jsonMessage.message, ClientIdMessage::class.java)

            if (clientIdMessage.clientId == "null")
                sendMessage(ClientIdMessage(UUID.randomUUID().toString()).toJsonMessage())
            else {
                clientId = clientIdMessage.clientId

                clientsController.clients[clientId] = this

                clientsController.clientsListener?.onClientIdentified(this)

                println("Client$clientId has been identified.")
            }

            return
        }

        clientsController.receiveMessage(clientId, jsonMessage)
    }

    fun sendMessage(jsonMessage: JsonMessage) : ChannelFuture? {
        return ctx?.writeAndFlush(jsonMessage)
    }

    override fun channelActive(ctx: ChannelHandlerContext?) {
        super.channelActive(ctx)

        this.ctx = ctx

        isActive = true

        clientsController.clientsListener?.onClientConnected(this)

        println("Client$clientId has connected.")
    }

    override fun exceptionCaught(ctx: ChannelHandlerContext?, cause: Throwable?) {
        super.exceptionCaught(ctx, cause)

        disconnect(REASON_KICKED_EXCEPTION)
        clientsController.removeClient(clientId)
    }

    override fun channelInactive(ctx: ChannelHandlerContext?) {
        super.channelInactive(ctx)

        this.ctx = null

        isActive = false

        clientsController.removeClient(clientId)

        if (!disconnected) {
            clientsController.clientsListener?.onClientLostConnection(this)
            logger.info("Client$clientId has lost connection.")
        }
        else {
            clientsController.clientsListener?.onClientDisconnected(this)
            logger.info("Client$clientId have been disconnected.")
        }
    }

    override fun channelRead(ctx: ChannelHandlerContext?, msg: Any?) {
        super.channelRead(ctx, msg)

        if (msg !is JsonMessage)
            return

        receiveMessage(msg)
    }
}
