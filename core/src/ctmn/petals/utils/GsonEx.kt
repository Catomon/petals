package ctmn.petals.utils

import com.google.gson.Gson
import com.google.gson.GsonBuilder
import com.google.gson.JsonElement
import ctmn.petals.unit.Ability
import ctmn.petals.utils.serialization.AbilityDeserializer

val gson: Gson = GsonBuilder().registerTypeAdapter(Ability::class.java, AbilityDeserializer()).setPrettyPrinting().create()
val gsonExp: Gson = GsonBuilder().registerTypeAdapter(Ability::class.java, AbilityDeserializer()).excludeFieldsWithoutExposeAnnotation().setPrettyPrinting().create()

val defaultGson = Gson()
val defaultPrettyGson: Gson = GsonBuilder().setPrettyPrinting().create()

fun <T> fromGson(json: String, clazz: Class<T>) : T {
    return defaultGson.fromJson(json, clazz)
}

fun <T> fromGson(json: JsonElement, clazz: Class<T>) : T {
    return defaultGson.fromJson(json, clazz)
}

fun Any.toGson() : String {
    return defaultGson.toJson(this, this::class.java)
}

fun Any.toPrettyGson() : String {
    return defaultPrettyGson.toJson(this, this::class.java)
}

fun Any.toGsonExcludeExpose() : String {
    return GsonBuilder().excludeFieldsWithoutExposeAnnotation().create().toJson(this, this::class.java)
}

fun Any.toPrettyGsonExcludeExpose() : String {
    return GsonBuilder().excludeFieldsWithoutExposeAnnotation().setPrettyPrinting().create().toJson(this, this::class.java)
}
