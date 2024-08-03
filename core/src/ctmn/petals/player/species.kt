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

fun getSpeciesBuildings(species: String): Array<Building> {
    return when (species) {
        fairy -> fairyUnits.buildings

        goblin -> goblinUnits.buildings

        else -> {
            TODO()
        }
    }
}

interface SpeciesUnits {
    val species: String

    val units: Array<UnitActor>

    val buildings: Array<Building>
}

class Building(val name: String, val buildTime: Int, val cost: Int, val terrains: Array<String>)

val fairyUnits = object : SpeciesUnits {
    override val species: String = fairy
    override val units: Array<UnitActor>
        get() = Array<UnitActor>().apply {
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

            add(FairyPeas().cost(1000))
            add(FairyHunter().cost(1200))
        }

    override val buildings: Array<Building> = Array<Building>().apply {
        add(Building("waterlily", 1, 25, Array<String>().apply { add("water") }))
        add(Building("plant_wall", 1, 50, Array<String>().apply { add("grass") }))
        add(Building("plant_tower", 1, 50, Array<String>().apply { add("grass") }))
    }
}

val goblinUnits = object : SpeciesUnits {
    override val species: String = goblin
    override val units: Array<UnitActor>
        get() = Array<UnitActor>().apply {
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

    override val buildings: Array<Building> = Array<Building>().apply {
        add(Building("bridge", 1, 25, Array<String>().apply { add("water") }))
        add(Building("wooden_wall", 1, 50, Array<String>().apply { add("grass") }))
        add(Building("wooden_tower", 1, 50, Array<String>().apply { add("grass") }))
    }
}

fun UnitActor.cost(value: Int): UnitActor {
    add(ShopComponent(value))

    return this
}

class SpeciesUnitNotFoundExc : Exception()