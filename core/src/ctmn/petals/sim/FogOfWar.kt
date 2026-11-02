package ctmn.petals.sim

import ctmn.petals.Config
import ctmn.petals.player.Player
import ctmn.petals.playstage.getCapturablesOf
import ctmn.petals.playstage.getMapSizedGridOfZero
import ctmn.petals.playstage.getMovementGrid
import ctmn.petals.playstage.getRangeOfView
import ctmn.petals.playstage.getTiles
import ctmn.petals.playstage.getUnitsForTeam
import ctmn.petals.tile.cPlayerId
import ctmn.petals.tile.isBase
import ctmn.petals.unit.TerrainPropsPack
import kotlin.Array as KArray

/**
 * Visibility rules for fog of war, shared by the client renderer and the dedicated server.
 *
 * The client draws the fog from this grid, and the server uses the same grid to decide what a
 * player is allowed to receive in a snapshot. Keeping it in one place is what makes the
 * filtered snapshots trustworthy.
 */
object FogOfWar {

    /**
     * A player with neither units nor capturable tiles: they have no vision sources at all, and
     * are only watching. A campaign player who owns no base yet but still has units is not a
     * spectator and must keep their fog.
     */
    fun isSpectator(field: BattleField, viewer: Player): Boolean =
        field.getCapturablesOf(viewer).isEmpty() && field.getUnitsForTeam(viewer.teamId).isEmpty()

    /**
     * @return a map sized grid where a positive value means the cell is visible to [viewer].
     */
    fun compute(field: BattleField, players: List<Player>, viewer: Player): KArray<IntArray> {
        val maxOfView = field.getMapSizedGridOfZero()

        if (isSpectator(field, viewer)) {
            for (row in maxOfView) row.fill(1)
            return maxOfView
        }

        for (unit in field.getUnitsForTeam(viewer.teamId)) {
            val unitViewRange = field.getRangeOfView(unit)
            for (i in maxOfView.indices) {
                for (j in maxOfView[i].indices) {
                    if (unitViewRange[i][j] > 0)
                        maxOfView[i][j] += unitViewRange[i][j]
                }
            }
        }

        // Vision of owned bases
        for (tile in field.getTiles()) {
            if (!tile.isBase) continue

            val owner = players.find { it.id == tile.cPlayerId?.playerId } ?: continue
            if (owner.teamId != viewer.teamId) continue

            val baseViewRange = field.getMovementGrid(
                Config.BASE_RANGE_OF_VIEW, tile.tiledX, tile.tiledY, TerrainPropsPack.view
            )
            for (i in maxOfView.indices) {
                for (j in maxOfView[i].indices) {
                    if (baseViewRange[i][j] > 0)
                        maxOfView[i][j] += baseViewRange[i][j]
                }
            }
        }

        return maxOfView
    }

    fun isVisible(visibility: KArray<IntArray>?, x: Int, y: Int): Boolean {
        if (visibility == null) return true
        if (x < 0 || y < 0) return false
        if (x >= visibility.size) return false
        if (visibility[x] == null || y >= visibility[x].size) return false

        return visibility[x][y] > 0
    }
}
