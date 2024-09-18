package ctmn.petals.bot

import ctmn.petals.playscreen.PlayScreen
import ctmn.petals.player.Player
import ctmn.petals.playscreen.*
import ctmn.petals.playscreen.seqactions.CameraMoveAction
import ctmn.petals.playscreen.commands.*
import ctmn.petals.tile.isOccupied
import ctmn.petals.unit.*
import ctmn.petals.unit.actors.creatures.BigEvilTree
import ctmn.petals.utils.tiledX
import ctmn.petals.utils.unTiled
import com.badlogic.gdx.Gdx
import com.badlogic.gdx.utils.Array
import ctmn.petals.playstage.*
import ctmn.petals.tile.cPlayerId
import ctmn.petals.unit.UnitActor
import ctmn.petals.utils.centerX
import ctmn.petals.utils.centerY

class EvilTreeBot(player: Player, playScreen: PlayScreen) : Bot(player, playScreen) {

    val unitsAwaitingOrders = Array<UnitActor>()
    val enemyUnits = Array<UnitActor>()

    private val buyPriority = Array<Pair<String, Int>>().apply {
        add(UnitIds.CATAPULT to 1)
        add(UnitIds.CROSSBOWMAN to 1)
        add(UnitIds.KNIGHT to 1)
        add(UnitIds.HORSEMAN to 1)
        add(UnitIds.AXEMAN to 1)
        add(UnitIds.BOWMAN to 1)
        add(UnitIds.SW0RDMAN to 2)
        add(UnitIds.SPEARMAN to 2)
    }

    private val idleTime = 0.5f
    private var curTime = 0f

    private var currentCommand: Command? = null

    private var didISayWaiting = false
    private var didISayNext = false

    private var moveCamera = true

    override fun update(delta: Float) {
        //Gdx.app.log(this::class.simpleName, "Update...")
        curTime += delta

        if (playScreen.actionManager.hasActions) {
            if (!didISayWaiting) {
                Gdx.app.log(this::class.simpleName, "Waiting for action complete...")

                didISayWaiting = true
            }

            curTime = 0f
            return
        }

        if (curTime < idleTime) return

        if (!didISayNext) {
            Gdx.app.log(this::class.simpleName, "Next Command...")
            didISayNext = true
        }

        val isCommandExecuted =
        nextCommand()

        if (isCommandExecuted) {

            onCommand()

            didISayWaiting = false
            didISayNext = false
        }
        
        isDone = curTime > 1.5 && playScreen.actionManager.isQueueEmpty
    }

    private fun nextCommand() : Boolean {
        unitsAwaitingOrders.clear()
        enemyUnits.clear()
        playScreen.playStage.getUnitsOfPlayer(player, unitsAwaitingOrders)
        playScreen.playStage.getUnitsOfEnemyOf(player, enemyUnits)

        if (useAbilityCommand()) return true
        if (buyCommand()) return true
        if (attackCommand()) return true
        if (moveCommand()) return true

        return false
    }

    private fun useAbilityCommand() : Boolean {
        for (unit in unitsAwaitingOrders) {
            if (unit.cAbilities?.abilities != null && !unit.cAbilities!!.abilities.isEmpty()) {
                val ability = unit.cAbilities!!.abilities.first()

                if (ability is BigEvilTree.SummonOwlsAbility) {
                    val command = UseAbilityCommand(
                        unit,
                        ability,
                        unit.tiledX,
                        unit!!.tiledY
                    )

                    if (command.canExecute(playScreen)) {
                        if (moveCamera)
                            playScreen.actionManager.queueAction(CameraMoveAction(unit.centerX, unit.centerY))
                        playScreen.commandManager.queueCommand(command, playerID)

                        currentCommand = command
                        return true
                    }
                }
            }
        }

        return false
    }

    private fun buyCommand() : Boolean {
        //check cash
        if (player.credits <= 0)
            return false

        //find base
        var hatchX: Int = -999
        var hatchY: Int = -999
        for (tile in playScreen.playStage.getTiles()) {
            if (tile.tileName.contains("base")) {
                if (tile.cPlayerId?.playerId == playerID) {
                    if (tile.isOccupied)
                        continue

                    hatchX = tile.tiledX
                    hatchY = tile.tiledY
                    break
                }
            }
        }
        if (hatchX < 0)
            return false

        //what unit to buy
        fun howMuchOfUnits(string: String, units: Array<UnitActor>) : Int {
            var count = 0
            for (unit in units) {
                if (unit.selfName == string)
                    count++
            }
            return count
        }

        var unitToBuy = UnitIds.SW0RDMAN

        for ((name, count) in buyPriority) {
            if (player.credits < (playScreen.unitsData.get(name).cShop?.price ?: continue)) continue

            if (howMuchOfUnits(name, playScreen.playStage.getUnitsOfPlayer(player)) < count) {
                unitToBuy = name
                break
            }
        }

        //if (unitToBuy.isEmpty()) return false // you've got enough of units

        //get price
        val unitPrice = playScreen.unitsData.get(unitToBuy).cShop?.price ?: -1 // yeah, kinda weird way

        //check cash
        if (player.credits < unitPrice)
            return false

        //buy
        val buyCommand = BuyUnitCommand(unitToBuy, player.id, unitPrice, hatchX, hatchY)
        if (buyCommand.canExecute(playScreen)) {
            if (moveCamera)
                playScreen.actionManager.queueAction(CameraMoveAction(hatchX.unTiled(), hatchY.unTiled()))
            playScreen.commandManager.queueCommand(buyCommand, playerID)

            return true
        }

        return false
    }

    private fun attackCommand() : Boolean {
        for (unit in unitsAwaitingOrders) {
            for (enemyUnit in enemyUnits) {
                if (unit.canAttackNow()) {
                    if (unit.inAttackRange(enemyUnit.tiledX, enemyUnit.tiledY)) {
                        val command = AttackCommand(unit, enemyUnit)

                        if (command.canExecute(playScreen)) {
                            if (moveCamera)
                                playScreen.actionManager.queueAction(CameraMoveAction(unit.centerX, unit.centerY))

                            playScreen.commandManager.queueCommand(command, playerID)
                            currentCommand = command

                            return true
                        }
                    }
                }
            }
        }

        return false
    }

    private fun moveCommand() : Boolean {
        for (unit in unitsAwaitingOrders) {
            if (!unit.canMove())
                continue

            // find the closest enemy unit
            var closestEnemyUnit = enemyUnits.firstOrNull() ?: break // break cos it means there is no units in the enemy team
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
                    if (movingMatrix[x][y] > 0 && playScreen.playStage.getTile(x, y)?.isOccupied != true) {
                        // дистанция между ближайшим юнитом врага и тайлом доступным для движения
                        if (tiledDst(closestEnemyUnit.tiledX, closestEnemyUnit.tiledY, x, y) < closestDst || closestDst == -1) {
                            closestDst = tiledDst(closestEnemyUnit.tiledX, closestEnemyUnit.tiledY, x, y)
                            closestTileX = x
                            closestTileY = y
                        }
                    }
                }
            }

            // do not move unit if it's >6 tiles away from the base and u also have less than 4 units
            if (closestTileX != -1) {
                if (!playScreen.playStage.getCapturablesOf(player).isEmpty)
                if (tiledDst(unit.tiledX, unit.tiledY, playScreen.playStage.getCapturablesOf(player).first().tiledX, playScreen.playStage.getCapturablesOf(player).first().tiledY) > 2)
                    if (playScreen.playStage.getUnitsOfPlayer(player).size < 5) {
                        unit.cUnit.actionPoints = 0
                        return true
                    }
            }

            // move to the closest to enemy unit tile
            if (closestTileX != -1 && closestTileY != -1) {

                val moveCommand = MoveUnitCommand(unit, closestTileX, closestTileY)

                if (moveCommand.canExecute(playScreen)) {
                    if (moveCamera)
                        playScreen.actionManager.queueAction(CameraMoveAction(unit.centerX, unit.centerY))
                    playScreen.commandManager.queueCommand(moveCommand, playerID)

                    currentCommand = moveCommand
                    return true
                }
            }

            // or move to the enemy base
//            val enemyBase = playScreen.playStage.getBaseOf(playScreen.playStage.getEnemiesOf(player).first())
//            if (enemyBase != null) {
//                println("nope")
//
//            } else return false
        }

        return false
    }

    private fun onCommand() {
        curTime = 0f
    }

    override fun onStart() {
        super.onStart()


    }

    override fun onEnd() {
        super.onEnd()

        unitsAwaitingOrders.clear()
        enemyUnits.clear()
    }
}