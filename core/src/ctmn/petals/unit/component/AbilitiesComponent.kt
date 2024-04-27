package ctmn.petals.unit.component

import com.badlogic.ashley.core.Component
import ctmn.petals.unit.Ability
import ctmn.petals.utils.CopyableComponent

data class AbilitiesComponent(
    var baseMana: Int = 100,
    val abilities: MutableSet<Ability> = mutableSetOf(),
    var mana: Int = baseMana,
) : Component, CopyableComponent {

    constructor(vararg abs: Ability) : this() {
        abilities.addAll(abs)
    }

    constructor(mana: Int, vararg abs: Ability) : this(*abs) {
        this.mana = mana
    }

    override fun makeCopy(): Component {
        return AbilitiesComponent(baseMana, mutableSetOf<Ability>().apply { addAll(this@AbilitiesComponent.abilities) }, mana)
    }
}