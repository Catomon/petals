package ctmn.petals.player

import com.badlogic.gdx.utils.Array
import ctmn.petals.unit.UnitActor
import ctmn.petals.unit.actors.fairies.*
import ctmn.petals.unit.actors.goblins.*
import ctmn.petals.unit.component.ShopComponent

const val fairy = "fairy"
const val goblin = "goblin"
val speciesList = arrayOf(fairy, goblin)

data class SpeciesUnit(
    val unitActor: UnitActor,
    val levelOfBase: Int,
    val requiredBuildings: List<String> = emptyList(),
)

fun getSpeciesUnits(species: String): Array<SpeciesUnit> {
    return when (species) {
        fairy -> fairySpecies.units

        goblin -> goblinSpecies.units

        else -> {
            TODO()
        }
    }
}

fun getSpeciesBuildings(species: String): Array<Building> {
    return when (species) {
        fairy -> fairySpecies.buildings

        goblin -> goblinSpecies.buildings

        else -> {
            TODO()
        }
    }
}

interface SpeciesUnits {
    val species: String

    val units: Array<SpeciesUnit>

    val buildings: Array<Building>
}

class Building(val name: String, val buildTime: Int, val cost: Int, val terrains: Array<String>, val requires: String = "")

const val FAIRY_FORGE = "fairy_forge"
const val FAIRY_ARCHERS_GUILD = "fairy_archers_guild"
const val FAIRY_ARMORY = "fairy_armory"
const val FAIRY_CUCUMBER = "fairy_cucumber"
const val FAIRY_FOLIA = "fairy_folia"
const val FAIRY_AERTHYS = "fairy_aerthys"

const val WATERLILY = "waterlily"
const val PLANT_WALL = "plant_wall"
const val PLANT_TOWER = "plant_tower"

//todo a building for healer and shield fairy

val fairySpecies = object : SpeciesUnits {
    override val species: String = fairy
    override val units: Array<SpeciesUnit>
        get() = Array<SpeciesUnit>().apply {
            fun add(unitActor: UnitActor, levelOfBase: Int, requiredBuildings: List<String> = emptyList()) {
                add(SpeciesUnit(unitActor, levelOfBase, requiredBuildings))
            }

            add(FairyPixie().cost(50), 1)

            add(FairySower().cost(100), 1)

            add(FairySword().cost(100), 1)
            add(FairyPike().cost(200), 1) //, listOf(FAIRY_FORGE)
            add(FairyBow().cost(300), 1, listOf(FAIRY_ARCHERS_GUILD))

            add(FairyHealer().cost(300), 1, listOf(FAIRY_FORGE))
            add(FairyShield().cost(300), 1, listOf(FAIRY_FORGE))

            add(FairyAxe().cost(400), 1, listOf(FAIRY_FORGE))
            add(FairyHammer().cost(400), 1, listOf(FAIRY_FORGE))
            add(FairyScout().cost(400), 1, listOf(FAIRY_FORGE, FAIRY_AERTHYS))

            add(FairyBomber().cost(500), 1, listOf(FAIRY_AERTHYS, FAIRY_CUCUMBER))
            add(FairyGlaive().cost(600), 1, listOf(FAIRY_FORGE, FAIRY_ARMORY))
            add(FairyArmorSword().cost(600), 1, listOf(FAIRY_FORGE, FAIRY_ARMORY))

            add(FairyWaterplant().cost(600), 1)
            add(FairyCucumber().cost(1000), 1, listOf(FAIRY_FOLIA, FAIRY_CUCUMBER))

            add(FairyPeas().cost(1000), 1, listOf(FAIRY_FOLIA, FAIRY_CUCUMBER))
            add(FairyHunter().cost(1200), 1, listOf(FAIRY_FOLIA))
        }

    override val buildings: Array<Building> = Array<Building>().apply {
        add(Building(FAIRY_FORGE, 2, 200, Array<String>().apply { add("grass") }))
        add(Building(FAIRY_ARCHERS_GUILD, 3, 200, Array<String>().apply { add("grass") }))
        add(Building(FAIRY_AERTHYS, 3, 300, Array<String>().apply { add("grass") }, FAIRY_FORGE))
        add(Building(FAIRY_CUCUMBER, 3, 300, Array<String>().apply { add("grass") }, FAIRY_AERTHYS))
        add(Building(FAIRY_ARMORY, 4, 400, Array<String>().apply { add("grass") }, FAIRY_FORGE))
        add(Building(FAIRY_FOLIA, 4, 400, Array<String>().apply { add("grass") }, FAIRY_ARMORY))

        add(Building(WATERLILY, 2, 50, Array<String>().apply { add("water", "swamp") }))
        add(Building(PLANT_WALL, 2, 100, Array<String>().apply { add("grass") }))
        add(Building(PLANT_TOWER, 2, 100, Array<String>().apply { add("grass") }, FAIRY_ARCHERS_GUILD))
    }
}

object fairyUnits {
    val units = fairySpecies.units.map { it.unitActor }
}

const val GOBLIN_FORGE = "goblin_forge"
const val GOBLIN_ARCHERS_GUILD = "goblin_archers_guild"
const val GOBLIN_ARMORY = "goblin_armory"
const val GOBLIN_BEASTMOUNT = "goblin_beastmount"
const val GOBLIN_FOUNDRY = "goblin_foundry"
const val GOBLIN_ARSENAL = "goblin_arsenal"

const val BRIDGE = "bridge"
const val WOODEN_WALL = "wooden_wall"
const val WOODEN_TOWER = "wooden_tower"

val goblinSpecies = object : SpeciesUnits {
    override val species: String = goblin
    override val units: Array<SpeciesUnit>
        get() = Array<SpeciesUnit>().apply {
            fun add(unitActor: UnitActor, levelOfBase: Int, requiredBuildings: List<String> = emptyList()) {
                add(SpeciesUnit(unitActor, levelOfBase, requiredBuildings))
            }

            add(GoblinScout().cost(50), 1)

            add(GoblinPickaxe().cost(100), 1)

            add(GoblinSword().cost(100), 1)
            add(GoblinPike().cost(200), 1) //, listOf(GOBLIN_FORGE)
            add(GoblinBow().cost(300), 1, listOf(GOBLIN_ARCHERS_GUILD))

            add(GoblinHealer().cost(300), 1, listOf(GOBLIN_FORGE))

            add(GoblinMace().cost(400), 1, listOf(GOBLIN_FORGE))
            add(GoblinWolf().cost(400), 1, listOf(GOBLIN_FORGE, GOBLIN_BEASTMOUNT))
            add(GoblinBoar().cost(400), 1, listOf(GOBLIN_FORGE))

            add(GoblinMachete().cost(500), 1, listOf(GOBLIN_FORGE, GOBLIN_ARMORY))
            add(GoblinBomber().cost(500), 1, listOf(GOBLIN_FORGE, GOBLIN_ARSENAL))

            add(GoblinCrossbow().cost(600), 1, listOf(GOBLIN_ARSENAL, GOBLIN_ARCHERS_GUILD))
            add(GoblinWyvern().cost(600), 1, listOf(GOBLIN_FORGE, GOBLIN_BEASTMOUNT))

            add(GoblinShip().cost(600), 1)
            add(GoblinGalley().cost(1000), 1, listOf(GOBLIN_FOUNDRY, GOBLIN_ARSENAL))

            add(GoblinCatapult().cost(1000), 1, listOf(GOBLIN_ARSENAL, GOBLIN_FOUNDRY))
            add(GoblinGiant().cost(1200), 1, listOf(GOBLIN_ARMORY, GOBLIN_FOUNDRY))
        }

    override val buildings: Array<Building> = Array<Building>().apply {
        add(Building(GOBLIN_FORGE, 2, 200, Array<String>().apply { add("grass") }))
        add(Building(GOBLIN_ARCHERS_GUILD, 3, 200, Array<String>().apply { add("grass") }))
        add(Building(GOBLIN_ARMORY, 3, 300, Array<String>().apply { add("grass") }, GOBLIN_FORGE))
        add(Building(GOBLIN_BEASTMOUNT, 3, 300, Array<String>().apply { add("grass") }, GOBLIN_FORGE))
        add(Building(GOBLIN_ARSENAL, 4, 400, Array<String>().apply { add("grass") }, GOBLIN_ARMORY))
        add(Building(GOBLIN_FOUNDRY, 4, 400, Array<String>().apply { add("grass") }, GOBLIN_ARSENAL))


        add(Building(BRIDGE, 2, 50, Array<String>().apply { add("water") }))
        add(Building(WOODEN_WALL, 2, 100, Array<String>().apply { add("grass") }))
        add(Building(WOODEN_TOWER, 2, 100, Array<String>().apply { add("grass") }, GOBLIN_ARCHERS_GUILD))
    }
}

object goblinUnits {
    val units = goblinSpecies.units.map { it.unitActor }
}

fun UnitActor.cost(value: Int): UnitActor {
    add(ShopComponent(value))

    return this
}

class SpeciesUnitNotFoundExc : Exception()