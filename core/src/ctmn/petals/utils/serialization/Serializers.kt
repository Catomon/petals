package ctmn.petals.utils.serialization

import com.google.gson.*
import ctmn.petals.unit.Abilities
import ctmn.petals.unit.Ability
import java.lang.reflect.Type

class AbilityDeserializer : JsonDeserializer<Ability> {

    override fun deserialize(json: JsonElement?, typeOfT: Type?, context: JsonDeserializationContext?): Ability {
        val jsonObject = json?.asJsonObject
        val name = jsonObject?.get("name")?.asString

        return Abilities.getAbility(name ?: throw JsonParseException("Ability name is null"))

//        return when (name) {
//            "healing" -> context?.deserialize(json, HealingAbility::class.java) as Ability
//            "flame" -> context?.deserialize(json, FlameAbility::class.java) as Ability
//            "summon" -> context?.deserialize(json, SummonAbility::class.java) as Ability
//            else -> throw JsonParseException("Unknown ability type: $name")
//        }
    }
}