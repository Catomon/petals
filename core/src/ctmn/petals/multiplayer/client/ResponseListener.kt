package ctmn.petals.multiplayer.client

import ctmn.petals.multiplayer.JsonMessage

open class ResponseListener(val messageId: String, val removeOnReceive: Boolean = true) {

    var isReceived = false

    var jsonMessage: JsonMessage? = null

    open fun responseReceived(jsonMessage: JsonMessage) {
        this.jsonMessage = jsonMessage
    }
}
