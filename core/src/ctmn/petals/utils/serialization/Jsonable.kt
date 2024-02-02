package ctmn.petals.utils.serialization

import com.google.gson.JsonObject

interface Jsonable {

    fun toJsonObject(): JsonObject

    fun fromJsonObject(json: JsonObject)
}