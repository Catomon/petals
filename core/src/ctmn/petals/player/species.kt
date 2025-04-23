package ctmn.petals.player

import com.badlogic.gdx.utils.Array
import ctmn.petals.playscreen.selfName
import ctmn.petals.unit.UnitActor
import ctmn.petals.unit.actors.fairies.*
import ctmn.petals.unit.actors.goblins.*
import ctmn.petals.unit.component.ShopComponent
import ctmn.petals.unit.cost

const val fairy = "fairy"
const val goblin = "goblin"

object Species {
    val names = arrayOf(fairy, goblin)

    const val FAIRY_FORGE = "fairy_forge"
    const val FAIRY_ARCHERS_GUILD = "fairy_archers_guild"
    const val FAIRY_ARMORY = "fairy_armory"
    const val FAIRY_CUCUMBER = "fairy_cucumber"
    const val FAIRY_FOLIA = "fairy_folia"
    const val FAIRY_AERTHYS = "fairy_aerthys"
    const val FAIRY_FLORAL_AEGIS = "fairy_floral_aegis"
    const val FAIRY_ACADEMY = "fairy_academy"

    const val WATERLILY = "waterlily"
    const val PLANT_WALL = "plant_wall"
    const val PLANT_TOWER = "plant_tower"

    val fairies = object : SpeciesUnits {
        override val species: String = fairy
        override val units: Map<String, SpeciesUnit> = mutableMapOf<String, SpeciesUnit>().apply {
            fun add(unitActor: UnitActor, levelOfBase: Int, requiredBuildings: List<String> = emptyList()) {
                put(unitActor.selfName, SpeciesUnit(unitActor, levelOfBase, requiredBuildings))
            }

            add(FairyPixie().cost(50), 1)

            add(FairySower().cost(100), 1)

            add(FairySword().cost(100), 1)
            add(FairyPike().cost(200), 1) //, listOf(FAIRY_FORGE)
            add(FairyBow().cost(300), 1, listOf(FAIRY_ARCHERS_GUILD))

            add(FairyHealer().cost(300), 1, listOf(FAIRY_FLORAL_AEGIS))
            add(FairyShield().cost(300), 1, listOf(FAIRY_FLORAL_AEGIS))

            add(FairyAxe().cost(400), 1, listOf(FAIRY_FORGE))
            add(FairyHammer().cost(400), 1, listOf(FAIRY_FORGE))
            add(FairyScout().cost(400), 1, listOf(FAIRY_FORGE, FAIRY_AERTHYS))

            add(FairyBomber().cost(500), 1, listOf(FAIRY_AERTHYS, FAIRY_CUCUMBER))
            add(FairyGlaive().cost(600), 1, listOf(FAIRY_FORGE, FAIRY_ARMORY))
            add(FairyArmorSword().cost(600), 1, listOf(FAIRY_FORGE, FAIRY_ARMORY))
            add(FairyMage().cost(700), 1, listOf(FAIRY_ACADEMY))

            add(FairyWaterplant().cost(600), 1)
            add(FairyCucumber().cost(1000), 1, listOf(FAIRY_FOLIA, FAIRY_CUCUMBER))

            add(FairyPeas().cost(1000), 1, listOf(FAIRY_FOLIA, FAIRY_CUCUMBER))
            add(FairyHunter().cost(1200), 1, listOf(FAIRY_FOLIA))
        }

        override val buildings: Array<Building> = Array<Building>().apply {
            add(Building(FAIRY_FORGE, 2, 200, Array<String>().apply { add("grass") }))
            add(Building(FAIRY_ARCHERS_GUILD, 3, 200, Array<String>().apply { add("grass") }))
            add(Building(FAIRY_FLORAL_AEGIS, 2, 200, Array<String>().apply { add("grass") }))
            add(Building(FAIRY_AERTHYS, 3, 300, Array<String>().apply { add("grass") }, FAIRY_FORGE))
            add(Building(FAIRY_CUCUMBER, 3, 300, Array<String>().apply { add("grass") }, FAIRY_AERTHYS))
            add(Building(FAIRY_ARMORY, 4, 400, Array<String>().apply { add("grass") }, FAIRY_FORGE))
            add(Building(FAIRY_FOLIA, 4, 400, Array<String>().apply { add("grass") }, FAIRY_ARMORY))
            add(Building(FAIRY_ACADEMY, 4, 400, Array<String>().apply { add("grass") }, FAIRY_FLORAL_AEGIS))

            add(Building(WATERLILY, 2, 50, Array<String>().apply { add("water", "swamp") }))
            add(Building(PLANT_WALL, 2, 100, Array<String>().apply { add("grass") }))
            add(Building(PLANT_TOWER, 2, 100, Array<String>().apply { add("grass") }, FAIRY_ARCHERS_GUILD))
        }
    }

    const val GOBLIN_FORGE = "goblin_forge"
    const val GOBLIN_ARCHERS_GUILD = "goblin_archers_guild"
    const val GOBLIN_ARMORY = "goblin_armory"
    const val GOBLIN_BEASTMOUNT = "goblin_beastmount"
    const val GOBLIN_FOUNDRY = "goblin_foundry"
    const val GOBLIN_ARSENAL = "goblin_arsenal"
    const val GOBLIN_AID_OUTPOST = "goblin_aid_outpost"
    const val GOBLIN_SORCERY_TOWER = "goblin_sorcery_tower"

    const val BRIDGE = "bridge"
    const val WOODEN_WALL = "wooden_wall"
    const val WOODEN_TOWER = "wooden_tower"

    val goblins = object : SpeciesUnits {
        override val species: String = goblin
        override val units: Map<String, SpeciesUnit> = mutableMapOf<String, SpeciesUnit>().apply {
            fun add(unitActor: UnitActor, levelOfBase: Int, requiredBuildings: List<String> = emptyList()) {
                put(unitActor.selfName, SpeciesUnit(unitActor, levelOfBase, requiredBuildings))
            }

            add(GoblinScout().cost(50), 1)

            add(GoblinPickaxe().cost(100), 1)

            add(GoblinSword().cost(100), 1)
            add(GoblinPike().cost(200), 1) //, listOf(GOBLIN_FORGE)
            add(GoblinBow().cost(300), 1, listOf(GOBLIN_ARCHERS_GUILD))

            add(GoblinHealer().cost(300), 1, listOf(GOBLIN_AID_OUTPOST))

            add(GoblinMace().cost(400), 1, listOf(GOBLIN_FORGE))
            add(GoblinWolf().cost(400), 1, listOf(GOBLIN_FORGE, GOBLIN_BEASTMOUNT))
            add(GoblinBoar().cost(400), 1, listOf(GOBLIN_FORGE))
            add(GoblinBoarPike().cost(400), 1, listOf(GOBLIN_FORGE))

            add(GoblinMachete().cost(500), 1, listOf(GOBLIN_FORGE, GOBLIN_ARMORY))
            add(GoblinBomber().cost(500), 1, listOf(GOBLIN_FORGE, GOBLIN_ARSENAL))

            add(GoblinCrossbow().cost(600), 1, listOf(GOBLIN_ARSENAL, GOBLIN_ARCHERS_GUILD))
            add(GoblinWyvern().cost(600), 1, listOf(GOBLIN_FORGE, GOBLIN_BEASTMOUNT))
            add(GoblinRatKing().cost(700), 1, listOf(GOBLIN_SORCERY_TOWER))

            add(GoblinShip().cost(600), 1)
            add(GoblinGalley().cost(1000), 1, listOf(GOBLIN_FOUNDRY, GOBLIN_ARSENAL))

            add(GoblinCatapult().cost(1000), 1, listOf(GOBLIN_ARSENAL, GOBLIN_FOUNDRY))
            add(GoblinGiant().cost(1200), 1, listOf(GOBLIN_ARMORY, GOBLIN_FOUNDRY))
        }

        override val buildings: Array<Building> = Array<Building>().apply {
            add(Building(GOBLIN_FORGE, 2, 200, Array<String>().apply { add("grass") }))
            add(Building(GOBLIN_ARCHERS_GUILD, 3, 200, Array<String>().apply { add("grass") }))
            add(Building(GOBLIN_AID_OUTPOST, 2, 200, Array<String>().apply { add("grass") }))
            add(Building(GOBLIN_ARMORY, 3, 300, Array<String>().apply { add("grass") }, GOBLIN_FORGE))
            add(Building(GOBLIN_BEASTMOUNT, 3, 300, Array<String>().apply { add("grass") }, GOBLIN_FORGE))
            add(Building(GOBLIN_ARSENAL, 4, 400, Array<String>().apply { add("grass") }, GOBLIN_ARMORY))
            add(Building(GOBLIN_FOUNDRY, 4, 400, Array<String>().apply { add("grass") }, GOBLIN_ARSENAL))
            add(Building(GOBLIN_SORCERY_TOWER, 4, 400, Array<String>().apply { add("grass") }, GOBLIN_AID_OUTPOST))

            add(Building(BRIDGE, 2, 50, Array<String>().apply { add("water") }))
            add(Building(WOODEN_WALL, 2, 100, Array<String>().apply { add("grass") }))
            add(Building(WOODEN_TOWER, 2, 100, Array<String>().apply { add("grass") }, GOBLIN_ARCHERS_GUILD))
        }
    }

    fun getSpeciesUnits(species: String): Collection<SpeciesUnit> {
        return when (species) {
            fairy -> fairies.units.values

            goblin -> goblins.units.values

            else -> {
                TODO()
            }
        }
    }

    fun getSpeciesBuildings(species: String): Array<Building> {
        return when (species) {
            fairy -> fairies.buildings

            goblin -> goblins.buildings

            else -> {
                TODO()
            }
        }
    }

    class SpeciesUnit constructor(
        val unitActor: UnitActor,
        val levelOfBase: Int,
        val requiredBuildings: List<String> = emptyList(),
    )

    interface SpeciesUnits {
        val species: String

        val units: Map<String, SpeciesUnit>

        val buildings: Array<Building>
    }

    class Building(
        val name: String,
        val buildTime: Int,
        val cost: Int,
        val terrains: Array<String>,
        val requires: String = "",
    )
}

@Deprecated("Species.Fairies.units")
object fairyUnits {
    val units = Species.fairies.units.map { it.value.unitActor }
}

@Deprecated("Species.Goblins.units")
object goblinUnits {
    val units = Species.goblins.units.map { it.value.unitActor }
}

class SpeciesUnitNotFoundExc : Exception()