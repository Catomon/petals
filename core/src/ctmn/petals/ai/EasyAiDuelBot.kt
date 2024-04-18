package ctmn.petals.ai

import ctmn.petals.playscreen.PlayScreen
import ctmn.petals.player.Player
import ctmn.petals.playscreen.*
import ctmn.petals.playscreen.seqactions.CameraMoveAction
import ctmn.petals.playscreen.commands.*
import ctmn.petals.unit.*
import ctmn.petals.unit.actors.SlimeHuge
import com.badlogic.gdx.Gdx
import com.badlogic.gdx.utils.Array
import ctmn.petals.player.getSpeciesUnits
import ctmn.petals.playscreen.seqactions.WaitAction
import ctmn.petals.playstage.*
import ctmn.petals.tile.*
import ctmn.petals.tile.TerrainNames
import ctmn.petals.tile.components.PlayerIdComponent
import ctmn.petals.unit.UnitActor
import ctmn.petals.utils.*
import ctmn.petals.utils.tiledX

class EasyAiDuelBot(player: Player, playScreen: PlayScreen) : AIBot(player, playScreen) {

    val unitsAwaitingOrders = Array<UnitActor>()
    val enemyUnits = Array<UnitActor>()

    private val speciesUnits = getSpeciesUnits(player.species)

    private val buyPriority = Array<Pair<String, Int>>().apply {

        speciesUnits.reversed().forEach { unit ->
            val amount = when (unit.cShop!!.price) {
                100 -> 3
                200 -> 2
                300 -> 1
                500 -> 1
                else -> 1
            }

            add(unit.selfName to amount)
        }
    }

    private val buyPriority2 = Array<Pair<String, Int>>().apply {
        speciesUnits.forEach { unit ->
            val amount = when (unit.cShop!!.price) {
                100 -> 4
                200 -> 3
                300 -> 2
                500 -> 2
                1500 -> 2
                2000 -> 4
                else -> 2
            }

            add(unit.selfName to amount)
        }
    }

    private val idleTime = 0.5f
    private var curTime = 0f

    private var currentCommand: Command? = null

    private var didISayWaiting = false
    private var didISayNext = false

    private var moveCamera = true

    private var captureCrystals = true

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

        isDone = curTime > 1 && playScreen.actionManager.isQueueEmpty
    }

    private fun nextCommand(): Boolean {
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

    private fun useAbilityCommand(): Boolean {
        for (unit in unitsAwaitingOrders) {
            if (unit.cAbilities?.abilities != null && !unit.cAbilities!!.abilities.isEmpty()) {
                val ability = unit.cAbilities!!.abilities.first()

                if (ability is SlimeHuge.SlimeJumpAbility) {
                    var freeTile: TileActor? = null
                    var enemyUnit: UnitActor? = playScreen.playStage.getUnits().apply {
                        removeAll {
                            it == unit
                                    || it.isAlly(player)
                        }
                    }.firstOrNull {
                        freeTile = playScreen.playStage.getSurroundingTiles(it.tiledX, it.tiledY, true)
                            .firstOrNull { tile -> unit.isInRange(tile.tiledX, tile.tiledY, ability.range) }
                        freeTile != null
                    } ?: continue

                    if (freeTile == null) continue

                    val command = UseAbilityCommand(
                        unit,
                        ability,
                        freeTile!!.tiledX,
                        freeTile!!.tiledY
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

    private fun buyCommand(): Boolean {
        //check cash
        if (player.credits <= 0)
            return false

        //find base
        var baseX: Int = -999
        var baseY: Int = -999
        for (tile in playScreen.playStage.getTiles()) {
            if (tile.terrain == TerrainNames.base) {
                if (tile.cPlayerId?.playerId == playerID) {
                    if (tile.isOccupied)
                        continue

                    baseX = tile.tiledX
                    baseY = tile.tiledY
                    break
                }
            }
        }
        if (baseX < 0)
            return false

        //what unit to buy
        fun howMuchOfUnits(string: String, units: Array<UnitActor>): Int {
            var count = 0
            for (unit in units) {
                if (unit.selfName == string)
                    count++
            }
            return count
        }

        var unitToBuy = ""

        for ((name, count) in buyPriority) {
            if (player.credits < (speciesUnits.find { it.selfName == name }?.cShop?.price ?: continue)) continue

            if (howMuchOfUnits(name, playScreen.playStage.getUnitsOfPlayer(player)) < count) {
                unitToBuy = name
                break
            }
        }

        val unitsToBuy = Array<String>()
        for ((name, count) in buyPriority) {
            if (howMuchOfUnits(name, playScreen.playStage.getUnitsOfPlayer(player)) < count)
                unitsToBuy.add(name)
        }

        if (unitToBuy.isEmpty()) {
            if (unitsToBuy.isEmpty) {
                unitToBuy = speciesUnits.first().selfName
                for ((name, count) in buyPriority2) {
                    if (howMuchOfUnits(name, playScreen.playStage.getUnitsOfPlayer(player)) < count)
                        unitsToBuy.add(name)
                }
            }
            if (!unitsToBuy.isEmpty)
                unitToBuy = unitsToBuy.last()
        }

        //get price
        val unitPrice = speciesUnits.find { it.selfName == unitToBuy }?.cShop?.price ?: -1

        //check cash
        if (player.credits < unitPrice)
            return false

        //buy
        val buyCommand = BuyUnitCommand(unitToBuy, player, unitPrice, baseX, baseY)
        if (buyCommand.canExecute(playScreen)) {
            if (moveCamera)
                playScreen.actionManager.queueAction(CameraMoveAction(baseX.unTiled(), baseY.unTiled()))

            playScreen.commandManager.queueCommand(buyCommand, playerID)

            return true
        }

        return false
    }

    private fun attackCommand(): Boolean {
        for (unit in unitsAwaitingOrders) {
            for (enemyUnit in enemyUnits) {
                if (unit.canAttack()) {
                    if (unit.inAttackRange(enemyUnit.tiledX, enemyUnit.tiledY)) {
                        val command = AttackCommand(unit, enemyUnit)

                        if (command.canExecute(playScreen)) {
                            if (moveCamera)
                                playScreen.actionManager.queueAction(CameraMoveAction(unit.centerX, unit.centerY))

                            if (unit.actionPoints == 2)
                                playScreen.actionManager.queueAction(WaitAction(0.5f))
                            else
                                playScreen.actionManager.queueAction(WaitAction(0.30f))

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

    private fun moveCommand(): Boolean {
        for (unit in unitsAwaitingOrders) {
            if (captureCrystals) {
                if (moveNCaptureCrystal(unit)) return true
            }

            if (!unit.canMove())
                continue

            // find the closest enemy unit
            var closestEnemyUnit =
                enemyUnits.firstOrNull() ?: break // break cos it means there is no units in the enemy team
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
                        if (tiledDst(
                                closestEnemyUnit.tiledX,
                                closestEnemyUnit.tiledY,
                                x,
                                y
                            ) < closestDst || closestDst == -1
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
                if (!playScreen.playStage.getCapturablesOf(player).isEmpty)
                    if (tiledDst(
                            unit.tiledX,
                            unit.tiledY,
                            playScreen.playStage.getCapturablesOf(player).first().tiledX,
                            playScreen.playStage.getCapturablesOf(player).first().tiledY
                        ) > 2
                    )
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

    private fun moveNCaptureCrystal(unit: UnitActor): Boolean {
        val playStage = playScreen.playStage

        // find the closest enemy unit
        var closestEnemyUnit = enemyUnits.firstOrNull()
        if (closestEnemyUnit != null) {
            for (enemyUnit in enemyUnits) {
                if (unit.distToUnit(enemyUnit) < unit.distToUnit(closestEnemyUnit!!)) {
                    closestEnemyUnit = enemyUnit
                }
            }
        }

        // find the closest crystal
        var closestCrystal = playStage.getTiles().filter {
            it.isCapturable && (it.get(PlayerIdComponent::class.java)?.playerId != unit.playerId || !it.has(
                PlayerIdComponent::class.java
            ))
        }.minByOrNull { tiledDst(unit.tiledX, unit.tiledY, it.tiledX, it.tiledY) } ?: return false

        // capture if on crystal
        if (unit.tiledX == closestCrystal.tiledX && unit.tiledY == closestCrystal.tiledY) {
            val command = CaptureCommand(unit, closestCrystal)
            if (command.canExecute(playScreen)) {
                if (moveCamera)
                    playScreen.actionManager.queueAction(CameraMoveAction(unit.centerX, unit.centerY))

                playScreen.queueCommand(command)
                return true
            }
        }

        if (!unit.canMove()) return false

        if (closestEnemyUnit != null && tiledDst(
                unit.tiledX,
                unit.tiledY,
                closestEnemyUnit.tiledX,
                closestEnemyUnit.tiledY
            ) < tiledDst(unit.tiledX, unit.tiledY, closestCrystal.tiledX, closestCrystal.tiledY)
        )
            return false

        // move to the closest crystal
        var closestTileX = -1
        var closestTileY = -1
        var closestDst = -1
        val movingMatrix = playScreen.playStage.getMovementGrid(unit, true)
        for (x in movingMatrix.indices) {
            for (y in 0 until movingMatrix[x].size) {
                if (movingMatrix[x][y] > 0 && playScreen.playStage.getTile(x, y)?.isOccupied != true) {
                    // дистанция между crystal врага и тайлом доступным для движения
                    if (tiledDst(closestCrystal.tiledX, closestCrystal.tiledY, x, y) < closestDst || closestDst == -1) {
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
                if (moveCamera)
                    playScreen.actionManager.queueAction(CameraMoveAction(unit.centerX, unit.centerY))
                playScreen.commandManager.queueCommand(moveCommand, playerID)

                currentCommand = moveCommand
                return true
            }
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