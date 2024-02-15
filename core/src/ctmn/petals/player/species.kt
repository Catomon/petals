package ctmn.petals.player

import com.badlogic.gdx.utils.Array
import ctmn.petals.unit.UnitActor
import ctmn.petals.unit.actors.*
import ctmn.petals.unit.component.ShopComponent

const val fairy = "fairy"
const val goblin = "goblin"

fun getSpeciesUnits(species: String): Array<UnitActor> {
    return when (species) {
        fairy -> fairyUnits.units

        goblin -> goblinUnits.units

        else -> {
            TODO()
        }
    }
}

interface SpeciesUnits {
    val species: String

    val units: Array<UnitActor>
}

val fairyUnits = object : SpeciesUnits {
    override val species: String = fairy
    override val units: Array<UnitActor> = Array<UnitActor>().apply {
        add(FairySword().cost(100))
        add(FairyPike().cost(150))
        add(FairyBow().cost(250))
        add(FairyAxe().cost(400))
        add(FairyCucumber().cost(1000))
    }
}

val goblinUnits = object : SpeciesUnits {
    override val species: String = goblin
    override val units: Array<UnitActor> = Array<UnitActor>().apply {
        add(GoblinSword().cost(100))
        add(GoblinPike().cost(150))
        add(GoblinBow().cost(250))
        add(GoblinBoar().cost(400))
        add(GoblinCatapult().cost(1000))
    }
}

fun UnitActor.cost(value: Int): UnitActor {
    add(ShopComponent(value))

    return this
}