package ctmn.petals.bot

import com.badlogic.gdx.Gdx
import com.badlogic.gdx.utils.Array
import ctmn.petals.Const
import ctmn.petals.player.Player
import ctmn.petals.player.getSpeciesUnits
import ctmn.petals.playscreen.*
import ctmn.petals.playscreen.commands.*
import ctmn.petals.playscreen.seqactions.CameraMoveAction
import ctmn.petals.playscreen.seqactions.WaitAction
import ctmn.petals.playstage.*
import ctmn.petals.tile.*
import ctmn.petals.tile.components.PlayerIdComponent
import ctmn.petals.unit.*
import ctmn.petals.unit.abilities.HealingTouchAbility
import ctmn.petals.unit.abilities.HealthPotionAbility
import ctmn.petals.unit.actors.SlimeHuge
import ctmn.petals.utils.*
import ctmn.petals.utils.tiledX
import kotlin.concurrent.thread
import kotlin.math.min

class EasyDuelBot(player: Player, playScreen: PlayScreen) : Bot(player, playScreen) {

    val unitsAwaitingOrders = Array<UnitActor>()
    val enemyUnits = Array<UnitActor>()

    private val speciesUnits = getSpeciesUnits(player.species)

    private val buyPriority = Array<Pair<String, Int>>().apply {

        add(speciesUnits[1].selfName to 2)

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

        add(speciesUnits[1].selfName to 3)

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

    private var lastActionTile = 0f

    private var isThinking = false
    private var isCommandExecuted = false

    private val thinkingThread get() = thread(false) {
        isThinking = true
        try {
            isCommandExecuted = nextCommand()
        } catch (e: Exception) {
            err("Bot exception nextCommand() " + e.message)
        }
        isThinking = false
    }

    override fun update(delta: Float) {
        if (isThinking) return

        curTime += delta
        lastActionTile += min(delta, 0.25f)

        if (playScreen.actionManager.hasActions) {
            if (!didISayWaiting) {
                Gdx.app.log(this::class.simpleName, "Waiting for action complete...")

                didISayWaiting = true
            }

            lastActionTile = 0f
            curTime = 0f
            return
        }

        if (curTime < idleTime) return

        if (!didISayNext) {
            Gdx.app.log(this::class.simpleName, "Next Command...")
            didISayNext = true
        }

        thinkingThread.start()

        if (isCommandExecuted && lastActionTile < 5f) {
            onCommand()

            didISayWaiting = false
            didISayNext = false
        } else {
            if (lastActionTile >= 5f)
                err("nextCommand returns true ban no commands executed")
        }

        isDone = curTime > 1 && playScreen.actionManager.isQueueEmpty
    }

    private fun doStuff(delta: Float) {
        //Gdx.app.log(this::class.simpleName, "Update...")
        curTime += delta
        lastActionTile += min(delta, 0.25f)

        if (playScreen.actionManager.hasActions) {
            if (!didISayWaiting) {
                Gdx.app.log(this::class.simpleName, "Waiting for action complete...")

                didISayWaiting = true
            }

            lastActionTile = 0f
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

        if (isCommandExecuted && lastActionTile < 5f) {

            onCommand()

            didISayWaiting = false
            didISayNext = false
        } else {
            if (lastActionTile >= 5f)
                err("nextCommand returns true ban no commands executed")
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

                when (ability) {
                    is SlimeHuge.SlimeJumpAbility -> {
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
                            playScreen.moveCameraToAction(unit.centerX, unit.centerY)

                            playScreen.commandManager.queueCommand(command, playerID)

                            currentCommand = command
                            return true
                        }
                    }

                    is HealthPotionAbility -> {
                        val teamUnit: UnitActor = playScreen.playStage.getUnits().filter {
                            it.isInRange(unit.tiledX, unit.tiledY, ability.range)
                        }.sortedBy { it.health }.firstOrNull { it.health < it.cUnit.baseHealth } ?: continue

                        val command = UseAbilityCommand(
                            unit,
                            ability,
                            teamUnit.tiledX,
                            teamUnit.tiledY
                        )

                        if (command.canExecute(playScreen)) {
                            playScreen.moveCameraToAction(unit.centerX, unit.centerY)

                            playScreen.commandManager.queueCommand(command, playerID)

                            currentCommand = command
                            return true
                        }
                    }

                    is HealingTouchAbility -> {
                        val teamUnit: UnitActor = playScreen.playStage.getUnits().filter {
                            it.isInRange(unit.tiledX, unit.tiledY, ability.range)
                        }.sortedBy { it.health }.firstOrNull { it.health < it.cUnit.baseHealth } ?: continue

                        val command = UseAbilityCommand(
                            unit,
                            ability,
                            teamUnit.tiledX,
                            teamUnit.tiledY
                        )

                        if (command.canExecute(playScreen)) {
                            playScreen.moveCameraToAction(unit.centerX, unit.centerY)

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

    private fun PlayScreen.moveCameraToAction(x: Float, y: Float) {
        if (moveCamera && fogOfWarManager.isVisible(x.tiled(), y.tiled())) actionManager.queueAction(
            CameraMoveAction(
                x,
                y
            )
        )
    }

    private fun buyCommand(): Boolean {
        //check cash
        if (player.credits <= 0)
            return false

        //find base
        var baseX: Int = -999
        var baseY: Int = -999

        for (tile in playScreen.playStage.getTiles()) {
            if (tile.isBase) {
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

        val baseTile = playScreen.playStage.getTile(baseX, baseY)
        val isWater = baseTile?.isWaterBase == true

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

        if (isWater && playScreen.playStage.getUnitsOfPlayer(playerID).filter { it.isWater }.size > 3)
            return false

        val buyPriority =
            if (isWater)
                buyPriority.filter { speciesUnits.find { unit -> unit.isWater }?.selfName == it.first }
            else
                buyPriority.filter { speciesUnits.find { unit -> unit.selfName == it.first && !unit.isWater } != null }
        val buyPriority2 =
            if (isWater)
                buyPriority
            else
                this.buyPriority2.filter { speciesUnits.find { unit -> unit.selfName == it.first && !unit.isWater } != null }

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

        // if (!unitsToBuy.isEmpty)
        //                if (isWater) return false
        //                else unitToBuy = this.buyPriority.filter { it.second in 100..600 }.random().first

        if (unitToBuy.isEmpty()) {
            if (unitsToBuy.isEmpty) {
                unitToBuy =
                    if (!isWater)
                        speciesUnits.first().selfName
                    else
                        buyPriority.filter { speciesUnits.find { it.isWater }?.selfName == it.first }
                            .minBy { it.second }.first
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
        val buyCommand = BuyUnitCommand(unitToBuy, player.id, unitPrice, baseX, baseY)
        if (buyCommand.canExecute(playScreen)) {
            playScreen.moveCameraToAction(baseX.unTiled(), baseY.unTiled())

            playScreen.commandManager.queueCommand(buyCommand, playerID)

            return true
        }

        return false
    }

    private fun attackCommand(): Boolean {
        for (unit in unitsAwaitingOrders) {
            if (unit.canBuildBase() || unit.selfName == UnitIds.DOLL_HEALER || unit.selfName == UnitIds.GOBLIN_HEALER) {
                return false
                //continue
            }

            for (enemyUnit in enemyUnits) {
                if (unit.canAttackNow()) {
                    if (unit.inAttackRange(enemyUnit.tiledX, enemyUnit.tiledY)) {
                        val command = AttackCommand(unit, enemyUnit)

                        if (command.canExecute(playScreen)) {
                            playScreen.moveCameraToAction(unit.centerX, unit.centerY)

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

            if (moveToClosestUnit(unit)) return true

            if (!enemyUnits.isEmpty) {
                val command = unit.moveTowardsCommand(enemyUnits.first().tiledX, enemyUnits.first().tiledX)
                if (command?.canExecute(playScreen) == true) {
                    playScreen.moveCameraToAction(unit.centerX, unit.centerY)

                    playScreen.commandManager.queueCommand(command, playerID)

                    return true
                }
            }
        }

        return false
    }

    private fun moveToClosestUnit(unit: UnitActor): Boolean {
        if (unit.cUnit.actionPoints < Const.ACTION_POINTS_MOVE_MIN) return false

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
                        return true
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
                    log("(MoveTClosestUnit) HE HAS BETTER TERRAIN IM NOT MOVING !!!")
                    return false
                } else {

                    val moveCommand = MoveUnitCommand(unit, closestTileX, closestTileY)

                    if (moveCommand.canExecute(playScreen)) {
                        playScreen.moveCameraToAction(unit.centerX, unit.centerY)

                        playScreen.commandManager.queueCommand(moveCommand, playerID)

                        currentCommand = moveCommand
                        return true
                    }
                }
            }
        }

        // or move to the enemy base
//            val enemyBase = playScreen.playStage.getBaseOf(playScreen.playStage.getEnemiesOf(player).first())
//            if (enemyBase != null) {
//                println("nope")
//
//            } else return false

        return false
    }

    private val incomeNeededPerBase = 300

    private fun moveNCaptureCrystal(unit: UnitActor): Boolean {
        val playStage = playScreen.playStage

        if (Const.EXPERIMENTAL) {
            if (unit.canBuildBase()) {
                if (player.credits >= Const.BASE_BUILD_COST) {
                    val incomeDiff = player.income(playScreen) / incomeNeededPerBase
                    if (incomeDiff >= 1) {
                        if (playStage.getCapturablesOf(player).filter { it.isBase }.size < 1 + (1 * incomeDiff)) {
                            val tile = playStage.getTile(unit.tiledX, unit.tiledY)
                            if (tile != null && unit.canBuildBase(tile)) {
                                val command = BuildBaseCommand(unit, tile)
                                if (command.canExecute(playScreen)) {
                                    playScreen.moveCameraToAction(unit.centerX, unit.centerY)

                                    playScreen.queueCommand(command)
                                    return true
                                }
                            }
                        }
                    }
                }
            }
        }

        if (!unit.canCapture()) {
            if (unit.canDestroy()) {
                if (!enemyUnits.isEmpty) {
                    if (playStage.getBases(playScreen.turnManager.getPlayerById(enemyUnits.first().playerId)!!).size != 0) {
                        if (moveNDestroyBase(unit)) return true
                    } else {
                        if (moveToClosestUnit(unit)) return true
                    }
                }
            }

            return false
        }

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
        val closestCrystal = playStage.getTiles().filter {
            it.isCrystal && (it.get(PlayerIdComponent::class.java)?.playerId != unit.playerId || !it.has(
                PlayerIdComponent::class.java
            ))
        }.minByOrNull { tiledDst(unit.tiledX, unit.tiledY, it.tiledX, it.tiledY) }

        if (closestCrystal == null) {
            val units = playStage.getUnitsOfPlayer(playerID)
            if (!units.isEmpty) {
                val unit2 = units.random()
                val command = unit.moveTowardsCommand(unit2.tiledX, unit2.tiledY)
                if (command != null) {
                    if (command.canExecute(playScreen)) {
                        playScreen.moveCameraToAction(unit.centerX, unit.centerY)

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
                playScreen.moveCameraToAction(unit.centerX, unit.centerY)

                playScreen.queueCommand(command)
                return true
            }
        }

        if (!unit.canMove()) return false

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
                playScreen.moveCameraToAction(unit.centerX, unit.centerY)

                playScreen.commandManager.queueCommand(moveCommand, playerID)

                currentCommand = moveCommand
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

    private fun moveNDestroyBase(unit: UnitActor): Boolean {
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
        val closestBase = playStage.getTiles().filter {
            it.isBase && (it.get(PlayerIdComponent::class.java)?.playerId != unit.playerId || !it.has(
                PlayerIdComponent::class.java
            ))
        }.minByOrNull { tiledDst(unit.tiledX, unit.tiledY, it.tiledX, it.tiledY) } ?: return false

        if (closestEnemyUnit != null) {
            if (tiledDst(
                    closestEnemyUnit.tiledX,
                    closestEnemyUnit.tiledY,
                    closestBase.tiledX,
                    closestBase.tiledY
                ) < 6
            ) {
                if (moveToClosestUnit(unit)) return true
            }
        }

        // capture if on crystal
        if (unit.tiledX == closestBase.tiledX && unit.tiledY == closestBase.tiledY) {
            val command = DestroyTileCommand(unit, closestBase)
            if (command.canExecute(playScreen)) {
                playScreen.moveCameraToAction(unit.centerX, unit.centerY)

                playScreen.queueCommand(command)
                return true
            }
        }

        if (!unit.canMove()) return false

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
                    if (tiledDst(closestBase.tiledX, closestBase.tiledY, x, y) < closestDst || closestDst == -1) {
                        closestDst = tiledDst(closestBase.tiledX, closestBase.tiledY, x, y)
                        closestTileX = x
                        closestTileY = y
                    }
                }
            }
        }

        // move to the closest to crystal tile
        if (closestTileX != -1 && closestTileY != -1) {
            if ((closestEnemyUnit?.cTerrainProps?.get(
                    playScreen.playStage.getTile(closestEnemyUnit)?.terrain ?: ""
                )?.atkPlusDf ?: 0) >
                (unit.cTerrainProps?.get(
                    playScreen.playStage.getTile(closestTileX, closestTileY)?.terrain ?: ""
                )?.atkPlusDf ?: 0)
            ) {
                log("(MoveNDestroyBase) HE HAS BETTER TERRAIN IM NOT MOVING !!!")
                return false
            } else {

                val moveCommand = MoveUnitCommand(unit, closestTileX, closestTileY)

                if (moveCommand.canExecute(playScreen)) {
                    playScreen.moveCameraToAction(unit.centerX, unit.centerY)

                    playScreen.commandManager.queueCommand(moveCommand, playerID)

                    currentCommand = moveCommand
                    return true
                }
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