package ctmn.petals.multiplayer.server

import ctmn.petals.multiplayer.JsonMessage
import com.google.gson.Gson
import kotlin.collections.ArrayDeque

class ClientRequestsQueue {

    val requestsQueue = ArrayDeque<Pair<String, JsonMessage>>()

    var clientsMessagesListener: ClientsMessagesListener? = null

    private val gson = Gson()

    fun performRequest(clientId: String, jsonMessage: JsonMessage) {
        clientsMessagesListener?.onClientMessage(clientId, jsonMessage)
    }

    interface ClientsMessagesListener {
        fun onClientMessage(clientId: String, jsonMessage: JsonMessage)
    }
}
