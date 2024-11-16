package ctmn.petals.unit

import com.badlogic.gdx.utils.ArrayMap
import kotlin.reflect.KClass
import ctmn.petals.unit.abilities.*

/**
 * This class is auto-generated.
 * [ctmn.petals.utils.ClassGen.generateUnitsClass]
 */

object Abilities : ArrayMap<String, KClass<out Ability>>() {

    init {
        add(EarthcrackAbility())
        add(ExhaustAbility())
        add(FireboltAbility())
        add(FlameAbility())
        add(FogAbility())
        add(FortressAbility())
        add(FreezeAbility())
        add(HammerAbility())
        add(HealingAbility())
        add(HealingTouchAbility())
        add(HealthPotionAbility())
        add(InspirationAbility())
        add(InvisibilityAbility())
        add(JumpAbility())
        add(LightningAbility())
        add(MeteoriteAbility())
        add(PersonalBarrierAbility())
        add(SummonAbility())
        add(TeleportAbility())
        add(UnsummonAbility())
    }

    private fun add(ability: Ability) {
        put(ability.name, ability::class)
    }
    fun getAbility(name: String): Ability {
        return get(name).java.constructors.first().newInstance() as Ability
    }
}
