package ctmn.petals.utils

import com.badlogic.gdx.utils.Array
import ctmn.petals.Const
import ctmn.petals.player.Player
import ctmn.petals.playstage.*
import ctmn.petals.tile.TileActor
import ctmn.petals.tile.isPassableAndFree
import ctmn.petals.unit.*
import ctmn.petals.unit.tiledX
import ctmn.petals.unit.tiledY
import kotlin.math.max

fun PlayStage.getUnitsInRange(tileX: Int, tileY: Int, range: Int): Array<UnitActor> {
    return getUnits().apply { removeAll { !it.isInRange(tileX, tileY, range) } }
}

fun PlayStage.getTilesInRange(tileX: Int, tileY: Int, range: Int, removeMid: Boolean = false): Array<TileActor> {
    val tiles = Array<TileActor>()
    getTiles().forEach { tile ->
        if (isInRange(tileX, tileY, range, tile.tiledX, tile.tiledY))
            tiles.add(tile)
    }

    if (removeMid) {
        tiles.removeAll { it.tiledX == tileX && it.tiledY == tileY }
    }

    return tiles
}

fun PlayStage.playerUnitsHasAction(player: Player): Array<UnitActor> {
    val units = Array<UnitActor>()

    getUnitsOfPlayer(player.id).forEach { myUnit ->
        if (myUnit.actionPoints > 0) {
            val tile = getTile(myUnit.tiledX, myUnit.tiledY)
            if (myUnit.actionPoints >= Const.ACTION_POINTS_MOVE_MIN) {
                units.add(myUnit)
            } else if (getUnitsOfEnemyOf(player).any { myUnit.canAttackNow(it) })
                units.add(myUnit)
            else if (tile?.let { myUnit.canCapture(it) } == true)
                units.add(myUnit)
            else if (tile?.let { myUnit.canBuildBase(it) } == true && player.credits >= Const.BASE_BUILD_COST)
                units.add(myUnit)
        }
    }

    return units
}

fun PlayStage.getTile(unitActor: UnitActor): TileActor? {
    return getTile(unitActor.tiledX, unitActor.tiledY)
}

fun PlayStage.getSurroundingTiles(tileX: Int, tileY: Int, isPassableAndFree: Boolean = false): Array<TileActor> {
    val tiles = Array<TileActor>(4)
    getTile(tileX, tileY - 1)?.let { tiles.add(it) }
    getTile(tileX, tileY + 1)?.let { tiles.add(it) }
    getTile(tileX - 1, tileY)?.let { tiles.add(it) }
    getTile(tileX + 1, tileY)?.let { tiles.add(it) }

    if (isPassableAndFree) tiles.removeAll { it?.isPassableAndFree() != true }

    return tiles
}

fun PlayStage.getSurroundingUnits(tileX: Int, tileY: Int): Array<UnitActor> {
    val units = Array<UnitActor>()
    getUnit(tileX, tileY - 1)?.let { units.add(it) }
    getUnit(tileX, tileY + 1)?.let { units.add(it) }
    getUnit(tileX - 1, tileY)?.let { units.add(it) }
    getUnit(tileX + 1, tileY)?.let { units.add(it) }

    return units
}

fun PlayStage.getSurroundingUnits(unitActor: UnitActor): Array<UnitActor> {
    return getSurroundingUnits(unitActor.tiledX, unitActor.tiledY)
}

fun PlayStage.getClosestTileInRange(
    startX: Int,
    startY: Int,
    destX: Int,
    destY: Int,
    pRange: Int,
    pTiles: Array<TileActor>? = null,
    increaseRange: Boolean = false,
    isPassableAndFree: Boolean = true,
): TileActor? {
    val playStage = this
    var closestTile: TileActor? = null
    val tiles = pTiles ?: playStage.getTiles()
    var range = pRange
    while ((closestTile == null || range * 2 < max(mapHeight(), mapWidth())) && increaseRange) {
        for (tile in tiles) {
            if (closestTile == null) {
                if (isInRange(
                        startX,
                        startY,
                        tile.tiledX,
                        tile.tiledY,
                        range
                    ) && (tile.isPassableAndFree() || !isPassableAndFree)
                )
                    closestTile = tile
            } else {
                // if tile is closer
                if (isInRange(
                        startX,
                        startY,
                        tile.tiledX,
                        tile.tiledY,
                        range
                    ) && (tile.isPassableAndFree() || !isPassableAndFree) && (tiledDst(
                        destX,
                        destY,
                        tile.tiledX,
                        tile.tiledY
                    ) < tiledDst(destX, destY, closestTile.tiledX, closestTile.tiledY))
                ) {
                    closestTile = tile
                }
            }
        }

        range++
    }

    return closestTile
}