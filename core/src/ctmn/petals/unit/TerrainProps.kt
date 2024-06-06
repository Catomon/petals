package ctmn.petals.unit

import ctmn.petals.tile.TerrainNames.land_capturable
import ctmn.petals.tile.TerrainNames.base
import ctmn.petals.tile.TerrainNames.chasm
import ctmn.petals.tile.TerrainNames.crystals
import ctmn.petals.tile.TerrainNames.deepwater
import ctmn.petals.tile.TerrainNames.earthcrack
import ctmn.petals.tile.TerrainNames.fallenforest
import ctmn.petals.tile.TerrainNames.impassable
import ctmn.petals.tile.TerrainNames.indoors
import ctmn.petals.tile.TerrainNames.grass
import ctmn.petals.tile.TerrainNames.highwall
import ctmn.petals.tile.TerrainNames.mountains
import ctmn.petals.tile.TerrainNames.roads
import ctmn.petals.tile.TerrainNames.forest
import ctmn.petals.tile.TerrainNames.fortress
import ctmn.petals.tile.TerrainNames.hills
import ctmn.petals.tile.TerrainNames.lava
import ctmn.petals.tile.TerrainNames.mud
import ctmn.petals.tile.TerrainNames.skyscraper
import ctmn.petals.tile.TerrainNames.swamp
import ctmn.petals.tile.TerrainNames.tower
import ctmn.petals.tile.TerrainNames.unwalkable
import ctmn.petals.tile.TerrainNames.walls
import ctmn.petals.tile.TerrainNames.water

const val UNREACHABLE = 999
const val ONE_TILE = 99

data class Terrain(
    val name: String,
    var movingCost: Int = 0,
    var attackBonus: Int = 0,
    var defenseBonus: Int = 0,
) {

    fun mv(mv: Int): Terrain {
        movingCost = mv
        return this
    }

    fun ad(at: Int, df: Int): Terrain {
        attackBonus = at
        defenseBonus = df

        return this
    }
}

class TerrainProps(
    private val terrains: HashMap<String, Terrain> = HashMap(),
) {

    constructor(vararg terrains: Terrain) : this() {
        terrains.forEach { this.terrains[it.name] = it }
    }

    operator fun set(key: String, value: Terrain) {
        terrains[key] = value
    }

    fun put(name: String, moveCost: Int = 0, attackBonus: Int = 0, defenseBonus: Int = 0) {
        put(Terrain(name, moveCost, attackBonus, defenseBonus))
    }

    fun put(terrain: Terrain): Terrain {
        terrains[terrain.name] = terrain
        return terrain
    }

    fun contains(name: String) = terrains.contains(name)

    operator fun get(name: String): Terrain = terrains[name] ?: put(Terrain(name))

    fun copy(): TerrainProps = TerrainProps().apply {
        this@TerrainProps.terrains.values.forEach {
            put(Terrain(it.name, it.movingCost, it.attackBonus, it.defenseBonus))
        }
    }
}

object TerrainPropsPack {
    val clear = TerrainProps().apply {
        put(grass, 0)
        put(forest, 0)
        put(mountains, 0)
        put(water, 0)
        put(roads, 0)
        put(walls, 0)
        put(impassable, 0)
        put(indoors, 0)
        put(highwall, 0)
        put("base", 0)
        put(hills, 0)
        put(earthcrack, 0)
        put(tower, 0)
        put(fortress, 0)
        put(fallenforest, 0)
    }

    val ability = TerrainProps().apply {
        put(grass, 0)
        put(forest, 0)
        put(mountains, 0)
        put(water, 0)
        put(roads, 0)
        put(walls, 0)
        put(impassable, 0)
        put(indoors, 0)
        put(highwall, 0)
        put("base", 0)
        put(hills, 0)
    }

    val view = TerrainProps().apply {
        put(grass, 0)
        put(forest, 2)
        put(mountains, 5)
        put(water, 0)
        put(roads, 0)
        put(walls, 3)
        put(impassable, 999)
        put(indoors, 0)
        put(highwall, 0)
        put("base", 0)
        put(hills, 0)
        put("fog", 2)
        put(earthcrack, 0)
        put(tower, 1)
        put(fortress, 1)
        put(fallenforest, 1)
        put(unwalkable, 0)
        put(crystals, 1)
        put(skyscraper, 5)
    }

    val horse = TerrainProps().apply {
        put(grass, 0, 0, 0)
        put(forest, 2, 0, 5)
        put(mountains, 3, 0, 10)
        put(water, 3, -15, -15)
        put(roads, 0, 0, 0)
        put(walls, 999, 0, 0)
        put(impassable, 999, 0, 0)
        put(indoors, 0, 0, 0)
        put(highwall, 999, 0, 0)
        put(base, 1, 0, 5)
        put(unwalkable, 999, 0, 5)
        put(earthcrack, 3, 0, -5)
        put(tower, 1, 0, 10)
        put(fortress, 1, 0, 15)
        put(fallenforest, 3, 0, -5)
        put(swamp, 3, -10, -10)
        put(hills, 1, 10, 0)
        put(crystals, 1)
        put(deepwater, UNREACHABLE, -30, -15)
        put(skyscraper, UNREACHABLE, -30, 15)
        put(chasm, UNREACHABLE)
        put(lava, ONE_TILE)
        put(mud, 1, -5, -5)
    }

    val foot = TerrainProps().apply {
        put(grass, 0, 0, 0)
        put(forest, 1, 5, 5)
        put(mountains, 2, 5, 10)
        put(water, 3, -5, -5)
        put(roads, 0, 0, 0)
        put(walls, 2, 5, 10)
        put(impassable, 999, 0, 0)
        put(indoors, 0, 0, 0)
        put(highwall, 999, 0, 0)
        put(base, 1, 0, 10)
        put(unwalkable, 999, 0, 0)
        put(hills, 1, 10, 0)
        put(earthcrack, 2, 0, -5)
        put(tower, 1, 5, 10)
        put(fortress, 1, 5, 15)
        put(fallenforest, 2, 0, 0)
        put(swamp, 3, -10, -5)
        put(crystals, 1, 0, 0)
        put(deepwater, UNREACHABLE, -15, -10)
        put(skyscraper, UNREACHABLE, -15, 10)
        put(chasm, UNREACHABLE)
        put(lava, ONE_TILE)
        put(mud, 1, -5, -5)
    }

    val flier = TerrainProps().apply {
        put(grass, 0)
        put(forest, 0)
        put(mountains, 0)
        put(water, 0)
        put(roads, 0)
        put(walls, 0)
        put(impassable, 999)
        put(indoors, 0)
        put(highwall, 0)
        put(base, 0)
        put(unwalkable, 0)
        put(hills, 0)
        put(tower, 0, 0, 10)
        put(fortress, 0, 0, 15)
        put(skyscraper, 1, 0, 15)
        put(chasm, 0)
        put(lava, 0)
        put(mud, 0)
    }

    //todo swimmer

    val waterOnly = TerrainProps().apply {
        put(grass, UNREACHABLE)
        put(forest, UNREACHABLE)
        put(mountains, UNREACHABLE)
        put(water, 0, 15, 5)
        put(deepwater, 0, 15, 15)
        put(roads, UNREACHABLE)
        put(walls, UNREACHABLE)
        put(impassable, UNREACHABLE)
        put(indoors, UNREACHABLE)
        put(highwall, UNREACHABLE)
        put(base, 1)
        put(hills, UNREACHABLE)
        put(earthcrack, UNREACHABLE)
        put(tower, UNREACHABLE)
        put(fortress, UNREACHABLE)
        put(fallenforest, UNREACHABLE)
        put(skyscraper, UNREACHABLE)
        put(chasm, UNREACHABLE)
        put(lava, ONE_TILE)
        put(land_capturable, UNREACHABLE)
        put(mud, UNREACHABLE)
    }

    val slime = TerrainProps().apply {
        put(grass, 0)
        put(forest, 1, 0, 5)
        put(mountains, 2, 0, 10)
        put(water, 2, 0, 5)
        put(roads, 0)
        put(walls, 2)
        put(impassable, 999)
        put(indoors, 0)
        put(highwall, 999)
        put(base, 1, 0, 10)
        put(unwalkable, 999)
        put(hills, 1, 0, 5)
        put(earthcrack, 2, 0, -5)
        put(tower, 1, 0, 10)
        put(fortress, 1, 0, 15)
        put(fallenforest, 1)
        put(swamp, 1, 10, 0)
        put(crystals, 1)
        put(deepwater, 2, 0, 5)
        put(skyscraper, UNREACHABLE, -15, 10)
        put(chasm, UNREACHABLE)
        put(lava, ONE_TILE)
        put(mud, 0, 5, 5)
    }

    val giant = TerrainProps().apply {
        put(base, ONE_TILE, 0, 5)
        put(crystals, ONE_TILE, 0, 0)
        put(grass, 0, 0, 0)
        put(forest, 2, 0, 5)
        put(mountains, 2, 0, 10)
        put(water, ONE_TILE, 0, -10)
        put(roads, -1, 0, 0)
        put(walls, ONE_TILE, 0, 10) //3
        put(impassable, 999, 0, 0)
        put(indoors, 0, 0, 0)
        put(highwall, 999, 0, 0)
        put(unwalkable, 999, 0, 0)
        put(hills, 2, 10, 0)
        put(earthcrack, 2, -10, -5)
        put(tower, ONE_TILE, -5, 5) //3
        put(fortress, ONE_TILE, -10, 10) //3
        put(fallenforest, 2, 0, 0)
        put(swamp, 2, -10, -10)
        put(deepwater, UNREACHABLE, -15, -15)
        put(skyscraper, UNREACHABLE, -15, 10)
        put(chasm, UNREACHABLE)
        put(lava, ONE_TILE)
        put(mud, 1, -5, -5)
    }
}