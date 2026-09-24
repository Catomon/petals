package ctmn.petals.bot.v2.util

import ctmn.petals.playstage.PlayStage
import ctmn.petals.playstage.getMovementGrid
import ctmn.petals.playstage.tiledDst
import ctmn.petals.tile.TileActor
import ctmn.petals.tile.isPassableAndFree
import ctmn.petals.unit.UnitActor
import ctmn.petals.unit.cTerrainProps
import ctmn.petals.unit.tiledX
import ctmn.petals.unit.tiledY

/** Greedy pathing on top of the game's own movement grid. */
object Pathing {

    /** Tiles the unit can reach this turn (excluding its own tile). */
    fun reachableTiles(playStage: PlayStage, unit: UnitActor): List<Pair<Int, Int>> {
        val grid = playStage.getMovementGrid(unit, true)
        val result = ArrayList<Pair<Int, Int>>()
        for (x in grid.indices) {
            for (y in grid[x].indices) {
                if (grid[x][y] <= 0) continue
                if (x == unit.tiledX && y == unit.tiledY) continue
                val tile = playStage.getTile(x, y) ?: continue
                if (!tile.isPassableAndFree()) continue
                result.add(x to y)
            }
        }
        return result
    }

    private fun terrainBonus(unit: UnitActor, tile: TileActor): Int =
        unit.cTerrainProps?.get(tile.terrain)?.let { it.attackBonus + it.defenseBonus } ?: 0

    /**
     * The tile the unit should step to in order to get closer to [destX]:[destY].
     * Prefers terrain that gives combat bonuses and, optionally, tiles away from enemies.
     */
    fun stepTowards(
        playStage: PlayStage,
        unit: UnitActor,
        destX: Int,
        destY: Int,
        preferTerrain: Boolean = true,
        avoidThreat: Boolean = false,
    ): TileActor? {
        val tiles = reachableTiles(playStage, unit)
        if (tiles.isEmpty()) return null

        val currentDist = tiledDst(unit.tiledX, unit.tiledY, destX, destY)
        val progressing = tiles.filter { tiledDst(it.first, it.second, destX, destY) < currentDist }
        val pool = progressing.ifEmpty { tiles }

        return pool.minWithOrNull(
            compareBy<Pair<Int, Int>>(
                { tiledDst(it.first, it.second, destX, destY) },
                {
                    if (!preferTerrain) 0
                    else {
                        val tile = playStage.getTile(it.first, it.second)
                        if (tile == null) 0 else -terrainBonus(unit, tile)
                    }
                },
                { if (avoidThreat) CombatEval.threatAt(unit, it.first, it.second) else 0 },
            )
        )?.let { playStage.getTile(it.first, it.second) }
    }

    /** Free (passable, unoccupied) tiles around [unit] within [range]. */
    fun freeTilesInRange(playStage: PlayStage, unit: UnitActor, range: Int): List<TileActor> {
        val tiles = ArrayList<TileActor>()
        for (x in (unit.tiledX - range)..(unit.tiledX + range)) {
            for (y in (unit.tiledY - range)..(unit.tiledY + range)) {
                if (tiledDst(unit.tiledX, unit.tiledY, x, y) > range) continue
                val tile = playStage.getTile(x, y) ?: continue
                if (tile.isPassableAndFree()) tiles.add(tile)
            }
        }
        return tiles
    }
}
