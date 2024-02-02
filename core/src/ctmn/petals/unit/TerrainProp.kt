package ctmn.petals.unit

import ctmn.petals.tile.Terrain.base
import ctmn.petals.tile.Terrain.earthcrack
import ctmn.petals.tile.Terrain.fallenforest
import ctmn.petals.tile.Terrain.impassable
import ctmn.petals.tile.Terrain.indoors
import ctmn.petals.tile.Terrain.grass
import ctmn.petals.tile.Terrain.highwall
import ctmn.petals.tile.Terrain.mountains
import ctmn.petals.tile.Terrain.roads
import ctmn.petals.tile.Terrain.forest
import ctmn.petals.tile.Terrain.fortress
import ctmn.petals.tile.Terrain.hills
import ctmn.petals.tile.Terrain.swamp
import ctmn.petals.tile.Terrain.tower
import ctmn.petals.tile.Terrain.unwalkable
import ctmn.petals.tile.Terrain.walls
import ctmn.petals.tile.Terrain.water

/** Terrain Properties used for setting terrain bonuses and movement for each unit separately */

object TerrainCosts {
    val clear = HashMap<String, Int>().apply {
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

    val ability = HashMap<String, Int>().apply {
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

    val view = HashMap<String, Int>().apply {
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
    }

    val horse = HashMap<String, Int>().apply {
        put(grass, 0)
        put(forest, 2)
        put(mountains, 3)
        put(water, 3)
        put(roads, 0)
        put(walls, 999)
        put(impassable, 999)
        put(indoors, 0)
        put(highwall, 999)
        put("base", 1)
        put(unwalkable, 999)
        put(hills, 0)
        put(earthcrack, 3)
        put(tower, 1)
        put(fortress, 1)
        put(fallenforest, 3)
    }

    val foot = HashMap<String, Int>().apply {
        put(grass, 0)
        put(forest, 1)
        put(mountains, 2)
        put(water, 3)
        put(roads, 0)
        put(walls, 2)
        put(impassable, 999)
        put(indoors, 0)
        put(highwall, 999)
        put("base", 1)
        put(unwalkable, 999)
        put(hills, 1)
        put(earthcrack, 2)
        put(tower, 1)
        put(fortress, 1)
        put(fallenforest, 1)
        put(swamp, 2)
    }

    val flier = HashMap<String, Int>().apply {
        put(grass, 0)
        put(forest, 0)
        put(mountains, 0)
        put(water, 0)
        put(roads, 0)
        put(walls, 0)
        put(impassable, 999)
        put(indoors, 0)
        put(highwall, 0)
        put("base", 0)
        put(unwalkable, 0)
        put(hills, 0)
    }

    val slime = HashMap<String, Int>().apply {
        put(grass, 0)
        put(forest, 1)
        put(mountains, 2)
        put(water, 2)
        put(roads, 0)
        put(walls, 2)
        put(impassable, 999)
        put(indoors, 0)
        put(highwall, 999)
        put("base", 1)
        put(unwalkable, 999)
        put(hills, 1)
        put(earthcrack, 2)
        put(tower, 1)
        put(fortress, 1)
        put(fallenforest, 1)
        put(swamp, 1)
    }
}

//first -> dmg, second -> def
object TerrainBuffs {

    val horse = HashMap<String, Pair<Int, Int>>().apply {
        put(grass, 0 to 0)
        put(forest, 0 to 5)
        put(mountains, 0 to 10)
        put(water, 0 to -5)
        put(roads, 0 to 0)
        put(walls, 0 to 0)
        put(impassable, 0 to 0)
        put(indoors, 0 to 0)
        put(highwall, 0 to 0)
        put(hills, 0 to 5)
        put(base, 0 to 5)
        put(earthcrack, 0 to -5)
        put(tower, 0 to 10)
        put(fortress, 0 to 15)
        put(fallenforest, 0 to -5)
        put(swamp, -10 to -10)
    }

    //todo
    val foot = HashMap<String, Pair<Int, Int>>().apply {
        put(grass, 0 to 0)
        put(forest, 0 to 5)
        put(mountains, 0 to 10)
        put(water, 0 to -5)
        put(roads, 0 to 0)
        put(walls, 0 to 0)
        put(impassable, 0 to 0)
        put(indoors, 0 to 0)
        put(highwall, 0 to 0)
        put(hills, 5 to 5)
        put(base, 0 to 10)
        put(earthcrack, 0 to -5)
        put(tower, 0 to 10)
        put(fortress, 0 to 15)
        put(fallenforest, 0 to 0)
        put(swamp, -5 to -5)
    }

    val fly = HashMap<String, Pair<Int, Int>>().apply {
        put(grass, 0 to 0)
        put(forest, 0 to 0)
        put(mountains, 0 to 0)
        put(water, 0 to 0)
        put(roads, 0 to 0)
        put(walls, 0 to 0)
        put(impassable, 0 to 0)
        put(indoors, 0 to 0)
        put(highwall, 0 to 0)
        put(hills, 0 to 0)
        put(base, 0 to 5)
        put(earthcrack, 0 to 0)
        put(tower, 0 to 10)
        put(fortress, 0 to 15)
        put(fallenforest, 0 to 0)
    }

    val slime = HashMap<String, Pair<Int, Int>>().apply {
        put(grass, 0 to 0)
        put(forest, 0 to 5)
        put(mountains, 0 to 10)
        put(water, 0 to 5)
        put(roads, 0 to 0)
        put(walls, 0 to 0)
        put(impassable, 0 to 0)
        put(indoors, 0 to 0)
        put(highwall, 0 to 0)
        put(hills, 0 to 5)
        put(base, 0 to 10)
        put(earthcrack, 0 to -5)
        put(tower, 0 to 10)
        put(fortress, 0 to 15)
        put(fallenforest, 0 to 0)
        put(swamp, 10 to 0)
    }
}
