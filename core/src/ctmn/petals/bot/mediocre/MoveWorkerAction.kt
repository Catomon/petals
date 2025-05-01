package ctmn.petals.bot.mediocre

import ctmn.petals.Const
import ctmn.petals.bot.BotAction
import ctmn.petals.player.Player
import ctmn.petals.playscreen.PlayScreen
import ctmn.petals.playscreen.commands.BuildBaseCommand
import ctmn.petals.playscreen.commands.CaptureCommand
import ctmn.petals.playscreen.commands.MoveUnitCommand
import ctmn.petals.playscreen.income
import ctmn.petals.playscreen.playStageOrNull
import ctmn.petals.playscreen.queueCommand
import ctmn.petals.playstage.*
import ctmn.petals.tile.*
import ctmn.petals.tile.components.PlayerIdComponent
import ctmn.petals.unit.*
import ctmn.petals.utils.tiledX

class MoveWorkerAction(val unit: UnitActor, private val bot: MidBot, val playScreen: PlayScreen) : BotAction() {

    private val playStage = playScreen.playStage
    private val player = bot.player
    private val playerId = player.id

    private val incomeNeededPerBase = 300

    override fun evaluate(): Int {
        priority = defaultPriority

        if (!unit.isWorker) return IMPOSSIBLE

        if (bot.botUnits.isEmpty()) return IMPOSSIBLE

        return if (unit.canMove()) defaultPriority else IMPOSSIBLE
    }

    override fun execute(): Boolean {
        val playStage = playScreen.playStage

        check(unit.isWorker)

        if (buildBase(playStage)) return true //todo build base action

        if (moveNCaptureCrystal(playStage)) return true

        if (moveToAlly()) return true

        return false
    }

    private fun moveToAlly(): Boolean {
        val nonWorkerAllies = bot.botUnits.values.filter { !it.isWorker }
        val hasAllyClose = nonWorkerAllies.any { unit.isInRange(it.tiledX, it.tiledY, 3) }

        if (!hasAllyClose) {
            nonWorkerAllies.maxByOrNull { tiledDst(it, unit) }?.let { allyUnit ->
                val command = allyUnit.moveTowardsCommand(allyUnit.tiledX, allyUnit.tiledY)
                if (command != null) {
                    if (command.canExecute(playScreen)) {
                        playScreen.queueCommand(command)
                        return true
                    }
                }
            }
        }

        return false
    }

    private fun buildBase(playStage: PlayStage): Boolean {
        if (unit.canBuildBase()) {
            if (player.credits >= Const.BASE_BUILD_COST) {
                val incomeDiff = player.income(playScreen) / incomeNeededPerBase
                if (incomeDiff >= 1) {
                    if (playStage.getCapturablesOf(player).filter { it.isBase }.size < 1 + (1 * incomeDiff)) {
                        val tile = playStage.getTile(unit.tiledX, unit.tiledY)
                        if (tile != null && unit.canBuildBase(tile)) {
                            val command = BuildBaseCommand(unit, tile)
                            if (command.canExecute(playScreen)) {
                                playScreen.queueCommand(command)
                                return true
                            }
                        }
                    }
                }
            }
        }
        return false
    }

    private fun moveNCaptureCrystal(playStage: PlayStage): Boolean {
        val crystalTiles =
            playStage.getTiles().filter { it.isCrystal && !player.isAllyId(it.cPlayerId?.playerId ?: Player.NONE) }

        if (crystalTiles.isEmpty()) return false

        // find the closest crystal
        val workers = playStage.getUnitsOfPlayer(player).filter { it.isWorker }
        val closestCrystal: TileActor? = playStage.getTiles().filter {
            it.isCrystal && (it.get(PlayerIdComponent::class.java)?.playerId != unit.playerId || !it.has(
                PlayerIdComponent::class.java
            ))
        }.minByOrNull { tiledDst(unit.tiledX, unit.tiledY, it.tiledX, it.tiledY) }.let {
            val unitToTile = workers.mapNotNull { playerUnit ->
                if (playerUnit != unit) {
                    playerUnit to crystalTiles.minBy { tiledDst(playerUnit, it) }
                } else null
            }

            crystalTiles.sortedBy { tiledDst(unit, it) }.forEach { crystalTile ->
                if (unitToTile.none { it.second == crystalTile } || unitToTile.firstOrNull { it.second == crystalTile }
                        ?.let { tiledDst(unit, it.second) <= tiledDst(it.first, it.second) } == true) {
                    return@let crystalTile
                }
            }

            it
        }

        if (closestCrystal == null) {
            val units = playStage.getUnitsOfPlayer(playerId)
            if (!units.isEmpty) {
                val unit2 = units.random()
                val command = unit.moveTowardsCommand(unit2.tiledX, unit2.tiledY)
                if (command != null) {
                    if (command.canExecute(playScreen)) {
                        playScreen.queueCommand(command)
                        return true
                    }
                }
            }

            //if (moveToClosestUnit(unit)) return true

            return false
        }

        // capture if on crystal
        if (unit.tiledX == closestCrystal.tiledX && unit.tiledY == closestCrystal.tiledY) {
            val command = CaptureCommand(unit, closestCrystal)
            if (command.canExecute(playScreen)) {
                playScreen.queueCommand(command)
                return true
            }
        }

        //        if (closestEnemyUnit != null && tiledDst(
        //                unit.tiledX,
        //                unit.tiledY,
        //                closestEnemyUnit.tiledX,
        //                closestEnemyUnit.tiledY
        //            ) < tiledDst(unit.tiledX, unit.tiledY, closestCrystal.tiledX, closestCrystal.tiledY)
        //        )
        //            return false

        // move to the closest crystal
        var closestTileX = -1
        var closestTileY = -1
        var closestDst = -1
        val movingMatrix = playScreen.playStage.getMovementGrid(unit, true)
        for (x in movingMatrix.indices) {
            for (y in 0 until movingMatrix[x].size) {
                if (movingMatrix[x][y] > 0 && playScreen.playStage.getTile(x, y)?.isOccupied != true) {
                    // дистанция между crystal врага и тайлом доступным для движения
                    if (tiledDst(
                            closestCrystal.tiledX,
                            closestCrystal.tiledY,
                            x,
                            y
                        ) < closestDst || closestDst == -1
                    ) {
                        closestDst = tiledDst(closestCrystal.tiledX, closestCrystal.tiledY, x, y)
                        closestTileX = x
                        closestTileY = y
                    }
                }
            }
        }

        // move to the closest to crystal tile
        if (closestTileX != -1 && closestTileY != -1) {
            val moveCommand = MoveUnitCommand(unit, closestTileX, closestTileY)

            if (moveCommand.canExecute(playScreen)) {
                playScreen.queueCommand(moveCommand)
                return true
            }
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