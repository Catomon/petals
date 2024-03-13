package ctmn.petals.player

import com.badlogic.gdx.utils.Array
import ctmn.petals.unit.UnitActor
import ctmn.petals.unit.actors.*
import ctmn.petals.unit.component.ShopComponent

const val fairy = "fairy"
const val goblin = "goblin"
val speciesList = arrayOf(fairy, goblin)

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
        add(FairyPike().cost(200))
        add(FairyBow().cost(300))
        add(FairyAxe().cost(500))
        add(FairyCucumber().cost(1500))
        add(FairyHunter().cost(2000))
    }
}

val goblinUnits = object : SpeciesUnits {
    override val species: String = goblin
    override val units: Array<UnitActor> = Array<UnitActor>().apply {
        add(GoblinSword().cost(100))
        add(GoblinPike().cost(200))
        add(GoblinBow().cost(300))
        add(GoblinBoar().cost(500))
        add(GoblinCatapult().cost(1500))
        add(GoblinGiant().cost(2000))
    }
}

fun UnitActor.cost(value: Int): UnitActor {
    add(ShopComponent(value))

    return this
}