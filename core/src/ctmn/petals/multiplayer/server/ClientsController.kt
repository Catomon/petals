package ctmn.petals.multiplayer.server

import ctmn.petals.multiplayer.JsonMessage
import io.netty.channel.ChannelFuture
import kotlin.collections.HashMap

class ClientsController(val clientRequestsQueue: ClientRequestsQueue) {

    val clients: HashMap<String, ClientHandler> = HashMap()

    var clientsListener: ClientsListener? = null

    companion object {
        const val REASON_SHUTDOWN = "shutdown"
        const val REASON_KICKED = "kicked"
        const val REASON_KICKED_EXCEPTION = "kicked_exception"
        const val REASON_DISCONNECTED_BY_CLIENT = "disconnected_by_client"
    }

    fun receiveMessage(clientId: String, jsonMessage: JsonMessage) {
        when (jsonMessage.id) {
            "disconnected" -> {

            }
        }

        clientRequestsQueue.performRequest(clientId, jsonMessage) // todo - move to separate thread
        //clientRequestsQueue.requestsQueue.addLast(clientId to jsonMessage)
    }

    // if you are wondering why are there clientId != "null" checks - they are not needed anymore.
    // It's cos I used to add new clients to the map as well, and they had "null" id before identification,
    // but then I added a check for non-valid uuid in the addClient function.
    fun broadcastMessage(jsonMessage: JsonMessage) {
        synchronized(clients) {
            for ((clientId, client) in clients) {
                if (clientId != "null")
                    client.sendMessage(jsonMessage)
            }
        }
    }

    fun broadcastExcept(exceptClientId: String, jsonMessage: JsonMessage) {
        synchronized(clients) {
            for ((clientId, client) in clients) {
                if (clientId != "null" && clientId != exceptClientId)
                    client.sendMessage(jsonMessage)
            }
        }
    }

    fun sendMessage(clientId: String, jsonMessage: JsonMessage) : ChannelFuture? {
        return clients[clientId]?.sendMessage(jsonMessage)
    }

    fun disconnectClient(clientId: String, reason: String) {
        clients[clientId]?.disconnect(reason)
        removeClient(clientId)
    }

    fun disconnectAllClients(reason: String) {
        clients.forEach {
            it.value.disconnect(reason)
        }

        synchronized(clients) {
            val values = clients.values.toList()
            for (value in values) {
                removeClient(value.clientId)
            }
        }
    }

    fun addClient(client: ClientHandler) : ClientHandler {
        // add if valid UUID
        if (client.clientId.split("_").size == 5)
            clients[client.clientId] = client

        clientsListener?.onClientConnected(client)

        return client
    }

    fun removeClient(clientId: String) {

        //clients[clientId]?.let { clientsListener?.onClientKicked(it) }

        clients.remove(clientId)
    }

    fun exists(id: String) : Boolean {
        return clients[id] != null
    }

    fun get(id: String) : ClientHandler? {
        return clients[id]
    }

    interface ClientsListener {

        fun onClientIdentified(client: ClientHandler) {}

        fun onClientConnected(client: ClientHandler) {}

        fun onClientLostConnection(client: ClientHandler) {}

        fun onClientDisconnected(client: ClientHandler) {}
    }
}
