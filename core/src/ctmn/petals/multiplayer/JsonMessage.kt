package ctmn.petals.multiplayer

import com.google.gson.JsonObject
import ctmn.petals.utils.fromGson
import ctmn.petals.utils.toGson

class JsonMessage(val message: String = "") {

    val id get() = fromGson(message, JsonObject::class.java).get("id")?.asString ?: ""

    override fun toString(): String {
        val messageMax100Chars = if (message.length > 100) message.substring(0, 100) else message
        return "id: $id, message: $messageMax100Chars ..."
    }
}

fun Any.toJsonMessage(): JsonMessage {
    return if (this is JsonMessage) this else JsonMessage(this.toGson())
}