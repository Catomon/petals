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
    override val units: Array<UnitActor> get() = Array<UnitActor>().apply {
        add(FairyPixie().cost(50))

        add(FairySower().cost(100))

        add(FairySword().cost(100))
        add(FairyPike().cost(200))
        add(FairyBow().cost(300))

        add(FairyHealer().cost(400))
        add(FairyShield().cost(400))

        add(FairyAxe().cost(500))
        add(FairyScout().cost(500))

        add(FairyBomber().cost(600))
        add(FairyGlaive().cost(600))
        add(FairyArmorSword().cost(600))

        add(FairyWaterplant().cost(600))
        add(FairyCucumber().cost(1000))

        add(FairyCannon().cost(1000))
        add(FairyHunter().cost(1200))
    }
}

val goblinUnits = object : SpeciesUnits {
    override val species: String = goblin
    override val units: Array<UnitActor> get() = Array<UnitActor>().apply {
        add(GoblinScout().cost(50))

        add(GoblinPickaxe().cost(100))

        add(GoblinSword().cost(100))
        add(GoblinPike().cost(200))
        add(GoblinBow().cost(300))

        add(GoblinHealer().cost(400))
        add(GoblinWolf().cost(400))

        add(GoblinBoar().cost(500))

        add(GoblinDuelist().cost(500))

        add(GoblinCrossbow().cost(600))
        add(GoblinWyvern().cost(600))

        add(GoblinShip().cost(600))
        add(GoblinGalley().cost(1000))

        add(GoblinCatapult().cost(1000))
        add(GoblinGiant().cost(1200))
    }
}

fun UnitActor.cost(value: Int): UnitActor {
    add(ShopComponent(value))

    return this
}

class SpeciesUnitNotFoundExc : Exception()