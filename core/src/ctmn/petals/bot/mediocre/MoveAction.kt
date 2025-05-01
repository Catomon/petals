package ctmn.petals.bot.mediocre

import ctmn.petals.bot.BotAction
import ctmn.petals.playscreen.PlayScreen
import ctmn.petals.playscreen.commands.MoveUnitCommand
import ctmn.petals.playscreen.playStageOrNull
import ctmn.petals.playstage.*
import ctmn.petals.tile.cPlayerId
import ctmn.petals.tile.isBase
import ctmn.petals.tile.isOccupied
import ctmn.petals.unit.*
import ctmn.petals.utils.getTile
import ctmn.petals.utils.tiledX

class MoveAction(val unit: UnitActor, private val bot: MidBot, val playScreen: PlayScreen) : BotAction() {

    private val playStage = playScreen.playStage
    private val player = bot.player
    private val playerId = player.id

    private val incomeNeededPerBase = 300

    override fun evaluate(): Int {
        priority = defaultPriority

        if (unit.isWorker) return IMPOSSIBLE

        if (bot.botUnits.isEmpty()) return IMPOSSIBLE

        return if (unit.canMove()) priority else IMPOSSIBLE
    }

    override fun execute(): Boolean {
        val playStage = playScreen.playStage

        //todo move n destroy bases

//        val target = bot.enemyUnits.values.filter { unit.canMoveAndAttackUnit(it) }.randomOrNull() ?: return false
//        val closestEnemyUnit = playStage.getUnitsOfEnemyOf(bot.player).minByOrNull { tiledDst(unit.tiledX, unit.tiledY, it.tiledX, it.tiledY) }
//        val target = closestEnemyUnit ?: return false
//        if (true) {
//            val cmd = unit.moveTowardsCommand(target.tiledX, target.tiledY) ?: return false
//            return playScreen.commandManager.queueCommand(cmd)
//        } else {
//            logErr("execute() failed. tried move unit: $unit.")
//        }

        val enemyUnits = bot.enemyUnits.values
        val player = bot.player
        // find the closest enemy unit
        var closestEnemyUnit =
            enemyUnits.firstOrNull() ?: return false
        for (enemyUnit in enemyUnits) {
            if (unit.distToUnit(enemyUnit) < unit.distToUnit(closestEnemyUnit)) {
                closestEnemyUnit = enemyUnit
            }
        }

        // move to the closest enemy unit
        var closestTileX = -1
        var closestTileY = -1
        var closestDst = -1
        val movingMatrix = playScreen.playStage.getMovementGrid(unit, true)
        for (x in movingMatrix.indices) {
            for (y in 0 until movingMatrix[x].size) {
                val tile = playScreen.playStage.getTile(x, y)
                if (movingMatrix[x][y] > 0 && tile?.isOccupied == false) {
                    // distance between the nearest enemy unit and the tile available for movement
                    if (tiledDst(
                            closestEnemyUnit.tiledX,
                            closestEnemyUnit.tiledY,
                            x,
                            y
                        ) < closestDst || closestDst == -1 || (unit.cTerrainProps?.get(
                            tile.terrain ?: ""
                        )?.atkPlusDf ?: 0) > (unit.cTerrainProps?.get(
                            playScreen.playStage.getTile(closestTileX, closestTileY)?.terrain ?: ""
                        )?.atkPlusDf ?: 0)
                    ) {
                        closestDst = tiledDst(closestEnemyUnit.tiledX, closestEnemyUnit.tiledY, x, y)
                        closestTileX = x
                        closestTileY = y
                    }
                }
            }
        }

        // do not move unit if it's >6 tiles away from the base and u also have less than 4 units
        if (closestTileX != -1) {
            if (!playScreen.playStage.getCapturablesOf(player).isEmpty) {
                val tile = playScreen.playStage.getTile(unit.tiledX, unit.tiledY)
                if (tiledDst(
                        unit.tiledX,
                        unit.tiledY,
                        playScreen.playStage.getBases(player).first().tiledX,
                        playScreen.playStage.getBases(player).first().tiledY,
                    ) > 2 && tile != null && !(tile.isBase && tile.cPlayerId?.playerId == unit.playerId)
                )
                    if (playScreen.playStage.getUnitsOfPlayer(player).size < 5) {
                        unit.cUnit.actionPoints = 1
                        return false
                    }
            }
        }

        // move to the closest to enemy unit tile
        if (closestTileX != -1 && closestTileY != -1) {
            if (tiledDst(closestTileX, closestTileY, closestEnemyUnit.tiledX, closestEnemyUnit.tiledY) == 1) {
                if ((closestEnemyUnit.cTerrainProps?.get(
                        playScreen.playStage.getTile(closestEnemyUnit)?.terrain ?: ""
                    )?.atkPlusDf ?: 0) >
                    (unit.cTerrainProps?.get(
                        playScreen.playStage.getTile(closestTileX, closestTileY)?.terrain ?: ""
                    )?.atkPlusDf ?: 0)
                ) {
                    return false
                } else {
                    val moveCommand = MoveUnitCommand(unit, closestTileX, closestTileY)

                    if (moveCommand.canExecute(playScreen)) {

                        return playScreen.commandManager.queueCommand(moveCommand)
                    }
                }
            }
        }

        val command = unit.moveTowardsCommand(enemyUnits.random().tiledX, enemyUnits.random().tiledY) ?: return false
        if (command.canExecute(playScreen)) {
            return playScreen.commandManager.queueCommand(command)
        }

        return false
    }

    private fun UnitActor.moveTowardsCommand(destX: Int, destY: Int): MoveUnitCommand? {
        playStageOrNull ?: return null
        val closestTile = getClosestTileInMoveRange(destX, destY)
        if (closestTile != null) return MoveUnitCommand(this, closestTile.tiledX, closestTile.tiledY)

        return null
    }
}