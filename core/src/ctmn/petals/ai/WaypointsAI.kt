package ctmn.petals.ai

import ctmn.petals.player.Player
import ctmn.petals.playscreen.*
import ctmn.petals.playscreen.commands.Command
import ctmn.petals.playscreen.commands.MoveUnitCommand
import ctmn.petals.tile.isOccupied
import ctmn.petals.unit.UnitActor
import ctmn.petals.unit.actionPoints
import ctmn.petals.utils.TilePosition
import com.badlogic.gdx.utils.Array
import ctmn.petals.playstage.getMovementGrid
import ctmn.petals.playstage.getUnitsOfPlayer
import ctmn.petals.playstage.tiledDst

class WaypointsAI(player: Player, playScreen: PlayScreen) : AIBot(player, playScreen) {

    // change to queue
    val waypoints = Array<TilePosition>()

    private var currentCommand: Command? = null

    val units = Array<UnitActor>()

    override fun update(delta: Float) {
        val units = if (this.units.isEmpty) playScreen.playStage.getUnitsOfPlayer(player) else this.units
        for (unit in units) {
            if (unit.actionPoints > 1) {
                moveToWaypointCommand(unit)

                break
            }
        }

        if (playScreen.actionManager.isQueueEmpty)
            done()
    }

    private fun moveToWaypointCommand(unit: UnitActor) {
        if (waypoints.isEmpty)
            return

        val waypoint = waypoints.first()

        var closestTileX = -1
        var closestTileY = -1
        var closestDst = -1
        val movingMatrix = playScreen.playStage.getMovementGrid(unit, true)
        for (x in movingMatrix.indices) {
            for (y in 0 until movingMatrix[x].size) {
                if (movingMatrix[x][y] > 0 && playScreen.playStage.getTile(x, y)?.isOccupied != true) {
                    // дистанция между ближайшим юнитом врага и тайлом доступным для движения
                    if (tiledDst(waypoint.x, waypoint.y, x, y) < closestDst || closestDst == -1) {
                        closestDst = tiledDst(waypoint.x, waypoint.y, x, y)
                        closestTileX = x
                        closestTileY = y
                    }
                }
            }
        }

        // move to the closest to waypoint tile
        if (closestTileX != -1 && closestTileY != -1) {

            val moveCommand = MoveUnitCommand(unit, closestTileX, closestTileY)

            if (moveCommand.canExecute(playScreen)) {
                val executed = playScreen.commandManager.queueCommand(moveCommand, playerID)

                if (executed) {
                    currentCommand = moveCommand

                    waypoints.removeValue(waypoint, false)
                }
            }
        }
    }
}