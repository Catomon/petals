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
import ctmn.petals.unit.actors.creatures.BunnySlimeHuge
import ctmn.petals.utils.*
import ctmn.petals.utils.tiledX
import kotlin.concurrent.thread
import kotlin.math.min

class EasyDuelBot(
    player: Player,
    playScreen: PlayScreen,
    private val speciesUnits: Array<UnitActor> = Array<UnitActor>().apply {
        getSpeciesUnits(player.species).forEach {
            add(it.unitActor)
        }
    },
) : Bot(player, playScreen) {

    val unitsAwaitingOrders = Array<UnitActor>()
    val enemyUnits = Array<UnitActor>()

    private val buyPriority = Array<Pair<String, Int>>().apply {
        add(speciesUnits[1].selfName to 2)

        speciesUnits.reversed().forEach { unit ->
            if (speciesUnits[1].selfName != unit.selfName) {
                val amount = when (unit.cShop!!.price) {
                    200 -> 1
                    100 -> 4
                    300 -> 1
                    500 -> 1
                    else -> 1
                }

                add(unit.selfName to amount)
            }
        }
    }

    private val buyPriority2 = Array<Pair<String, Int>>().apply {
        add(speciesUnits[1].selfName to 3)

        speciesUnits.forEach { unit ->
            if (speciesUnits[1].selfName != unit.selfName) {
                val amount = when (unit.cShop!!.price) {
                    200 -> 2
                    100 -> 5
                    300 -> 2
                    500 -> 2
                    1500 -> 2
                    2000 -> 4
                    else -> 2
                }

                add(unit.selfName to amount)
            }
        }
    }

    private val idleTime = 0.75f
    private var curTime = 0f

    private var currentCommand: Command? = null

    private var didISayWaiting = false
    private var didISayNext = false

    private var moveCamera = true

    private var captureCrystals = true

    private var lastActionTime = 0f

    private var isThinking = false
    private var isCommandExecuted = false

    private var cameraMoveAction: CameraMoveAction? = null

    private val thinkingThread
        get() = thread(false) {
            isThinking = true
            try {
                nextCommand()
            } catch (e: Exception) {
                logErr("Bot exception nextCommand() ")
                e.printStackTrace()
            }
            isThinking = false
        }

    override fun onStart() {
        super.onStart()

        curTime = 0f
        isThinking = false
        isCommandExecuted = false
        lastActionTime = 0f
        currentCommand = null
        didISayWaiting = false
        didISayNext = false
    }

    //temporary; replace idleTime with WaitAction
    private var halfCurTime = false

    override fun update(delta: Float) {
        if (isThinking) return
        if (isDone) return

        curTime += delta
        lastActionTime += min(delta, 0.25f)

        if (playScreen.actionManager.hasActions) {
            if (!didISayWaiting) {
                Gdx.app.log(this::class.simpleName, "Waiting for action complete...")

                didISayWaiting = true
            }

            lastActionTime = 0f

            curTime = if (halfCurTime) {
                idleTime * 1.5f / 1.5f
            } else {
                0f
            }

            return
        }

        if (curTime < idleTime) return

        if (!didISayNext) {
            Gdx.app.log(this::class.simpleName, "Next Command...")
            didISayNext = true
        }

        if (currentCommand != null) {
            if (cameraMoveAction != null) playScreen.actionManager.queueAction(cameraMoveAction!!)
            if (currentCommand is MoveUnitCommand) {
                if (!playScreen.fogOfWarManager.isVisible((currentCommand as MoveUnitCommand).tileX, (currentCommand as MoveUnitCommand).tileY))
                    halfCurTime = true
                else
                    halfCurTime = false
            }

            executeCommand(currentCommand!!)
            currentCommand = null
            isCommandExecuted = true
        } else {
            isThinking = true
            thinkingThread.start()
        }

        if (isCommandExecuted) {
            curTime = 0f
            didISayWaiting = false
            didISayNext = false
            isCommandExecuted = false
        }

        isDone = lastActionTime > 1.5f && curTime > idleTime * 1.5f && playScreen.actionManager.isQueueEmpty
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
                    is BunnySlimeHuge.SlimeJumpAbility -> {
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
                            playScreen.moveCameraAction(unit.centerX, unit.centerY)

                            queueCommand(command)

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
                            playScreen.moveCameraAction(unit.centerX, unit.centerY)

                            queueCommand(command)

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
                            playScreen.moveCameraAction(unit.centerX, unit.centerY)

                            queueCommand(command)

                            currentCommand = command
                            return true
                        }
                    }
                }
            }
        }

        return false
    }

    private fun queueCommand(command: Command) {
        this.currentCommand = command
    }

    private fun executeCommand(command: Command, playerId: Int? = null) {
        playScreen.commandManager.queueCommand(command, playerId ?: playerID)
    }

    private fun PlayScreen.moveCameraAction(x: Float, y: Float) {
        if (moveCamera && fogOfWarManager.isVisible(x.tiled(), y.tiled())) {
            cameraMoveAction = CameraMoveAction(x, y)
        }
    }

    private fun buyCommand(): Boolean {
        val playStage = playScreen.playStage

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

        val buyPriority: MutableList<Pair<String, Int>> =
            if (isWater)
                buyPriority.filter { speciesUnits.find { unit -> unit.isWater }?.selfName == it.first }.toMutableList()
            else
                buyPriority.filter { speciesUnits.find { unit -> unit.selfName == it.first && !unit.isWater } != null }
                    .toMutableList()
        val buyPriority2: MutableList<Pair<String, Int>> =
            if (isWater)
                buyPriority.toMutableList()
            else
                this.buyPriority2.filter { speciesUnits.find { unit -> unit.selfName == it.first && !unit.isWater } != null }
                    .toMutableList()

        //if there are enemies near the base in range of 2, don't buy worker units
        if (playScreen.playStage.getUnitsInRange(baseX, baseY, 2).any { !it.isAlly(player) }
            || playStage.getTiles().none { it.isCrystal }) {
            //1
            buyPriority.removeAll { it.first == UnitIds.DOLL_SOWER || it.first == UnitIds.GOBLIN_PICKAXE }

            //2
            buyPriority2.removeAll { it.first == UnitIds.DOLL_SOWER || it.first == UnitIds.GOBLIN_PICKAXE }
        }

        val enemyAirUnitsCount =
            (enemyUnits.count { it.isAir || it.selfName == UnitIds.DOLL_SCOUT || it.selfName == UnitIds.GOBLIN_WYVERN } * 0.65f).toInt()
        if (enemyAirUnitsCount > playStage.getUnitsOfPlayer(player).count { it.canAttackAir }) {
            //1
            buyPriority.removeAll { unitBuyPair -> speciesUnits.any { it.selfName == unitBuyPair.first && it.canAttackAir } }
            speciesUnits.filter { it.canAttackAir && (it.cShop?.price ?: -1) >= 100 }.sortedBy { it.cShop?.price ?: -1 }
                .forEach { unit ->
                    this.buyPriority.firstOrNull { it.first == unit.selfName }?.let { pair ->
                        buyPriority.add(0, pair.copy(second = enemyAirUnitsCount))
                    }
                }

            //2
            buyPriority2.removeAll { unitBuyPair -> speciesUnits.any { it.selfName == unitBuyPair.first && it.canAttackAir } }
            speciesUnits.filter { it.canAttackAir && (it.cShop?.price ?: -1) >= 100 }.sortedBy { it.cShop?.price ?: -1 }
                .forEach { unit ->
                    this.buyPriority.firstOrNull { it.first == unit.selfName }?.let { pair ->
                        buyPriority2.add(0, pair.copy(second = enemyAirUnitsCount))
                    }
                }
        }

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
            playScreen.moveCameraAction(baseX.unTiled(), baseY.unTiled())

            playScreen.actionManager.queueAction(WaitAction(0.30f))
            queueCommand(buyCommand)

            return true
        }

        return false
    }

    private fun attackCommand(): Boolean {
        for (unit in unitsAwaitingOrders) {
            if (unit.canBuildBase() || unit.selfName == UnitIds.DOLL_HEALER || unit.selfName == UnitIds.GOBLIN_HEALER) {
                continue
            }

            //move off of base if is yours and u have some credits
            if (playScreen.playStage.getTile(unit.tiledX, unit.tiledY)?.let {
                    it.isBase && it.cPlayerId?.playerId == unit.playerId
                } == true && player.credits > 100) {
                return false
            }

            for (enemyUnit in enemyUnits) {
                if (unit.canAttackNow()) {
                    if (unit.inAttackRange(enemyUnit.tiledX, enemyUnit.tiledY)) {
                        val command = AttackCommand(unit, enemyUnit)

                        if (command.canExecute(playScreen)) {
                            playScreen.moveCameraAction(unit.centerX, unit.centerY)

                            if (unit.actionPoints == 2) {
                                playScreen.queueAction {
                                    playScreen.guiStage.selectUnit(unit)
                                }
                                playScreen.actionManager.queueAction(WaitAction(0.20f))
                            }
//                            else
//                                playScreen.actionManager.queueAction(WaitAction(0.30f))

                            queueCommand(command)
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
                val command = unit.moveTowardsCommand(enemyUnits.first().tiledX, enemyUnits.first().tiledY)
                if (command?.canExecute(playScreen) == true) {
                    playScreen.queueAction {
                        playScreen.guiStage.selectUnit(unit)
                    }
                    playScreen.moveCameraAction(unit.centerX, unit.centerY)
                    if (playScreen.fogOfWarManager.isVisible(command.tileX, command.tileY))
                        playScreen.queueAction(WaitAction(0.30f))

                    queueCommand(command)

                    return true
                }
            }
        }

        return false
    }

    private fun closestEnemyBase(unit: UnitActor): TileActor? {
        val bases = playScreen.playStage.getBases().filter { !player.isAllyId(it?.cPlayerId?.playerId ?: Player.NONE) }
        var closestBase = bases.firstOrNull() ?: return null
        for (base in bases) {
            if (tiledDst(base.tiledX, base.tiledY, unit.tiledX, unit.tiledY) < tiledDst(
                    closestBase.tiledX,
                    closestBase.tiledY,
                    unit.tiledX,
                    unit.tiledY
                )
            ) {
                closestBase = base
            }
        }

        return closestBase
    }

    private fun closestBase(unit: UnitActor): TileActor? {
        val bases = playScreen.playStage.getBases(player)
        var closestBase = bases.firstOrNull() ?: return null
        for (base in bases) {
            if (tiledDst(base.tiledX, base.tiledY, unit.tiledX, unit.tiledY) < tiledDst(
                    closestBase.tiledX,
                    closestBase.tiledY,
                    unit.tiledX,
                    unit.tiledY
                )
            ) {
                closestBase = base
            }
        }

        return closestBase
    }

    private fun closestEnemy(unit: UnitActor): UnitActor? {
        var closestEnemyUnit =
            enemyUnits.firstOrNull() ?: return null
        for (enemyUnit in enemyUnits) {
            if (unit.distToUnit(enemyUnit) < unit.distToUnit(closestEnemyUnit)) {
                closestEnemyUnit = enemyUnit
            }
        }

        return closestEnemyUnit
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
                val tile = playScreen.playStage.getTile(x, y)
                if (movingMatrix[x][y] > 0 && tile?.isOccupied == false) {
                    // дистанция между ближайшим юнитом врага и тайлом доступным для движения
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
                        playScreen.moveCameraAction(unit.centerX, unit.centerY)

                        if (playScreen.fogOfWarManager.isVisible(moveCommand.tileX, moveCommand.tileY))
                            playScreen.queueAction(WaitAction(0.30f))

                        queueCommand(moveCommand)

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
                                    playScreen.moveCameraAction(unit.centerX, unit.centerY)

                                    queueCommand(command)
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
                var moreCloseToEnemyBaseThanHis = false
                val enemyNearClosestBase = closestBase(unit)?.let { closestBase ->
                    closestEnemy(unit)?.let { closestEnemy ->
                        val enemyToMyBaseDist = tiledDst(closestEnemy, closestBase)

                        moreCloseToEnemyBaseThanHis = closestEnemyBase(unit)?.let { enemyBase ->
                            tiledDst(unit, enemyBase) < enemyToMyBaseDist
                        } == true

                        enemyToMyBaseDist <= 6
                    }
                } == true
                //playScreen.turnManager.getPlayerById(enemyUnits.first().playerId)!!
                if (playStage.getBasesOfEnemyOf(player).size != 0
                    && (!enemyNearClosestBase || moreCloseToEnemyBaseThanHis)
                ) {
                    if (moveNDestroyBase(unit)) return true
                } else {
                    if (!enemyUnits.isEmpty)
                        if (moveToClosestUnit(unit)) return true
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

        val crystalTiles =
            playStage.getTiles().filter { it.isCrystal && !player.isAllyId(it.cPlayerId?.playerId ?: Player.NONE) }

        if (crystalTiles.isEmpty()) {
            return moveToClosestUnit(unit)
        }

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
            val units = playStage.getUnitsOfPlayer(playerID)
            if (!units.isEmpty) {
                val unit2 = units.random()
                val command = unit.moveTowardsCommand(unit2.tiledX, unit2.tiledY)
                if (command != null) {
                    if (command.canExecute(playScreen)) {
                        playScreen.moveCameraAction(unit.centerX, unit.centerY)

                        queueCommand(command)
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
                playScreen.moveCameraAction(unit.centerX, unit.centerY)

                queueCommand(command)
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
                playScreen.moveCameraAction(unit.centerX, unit.centerY)

                queueCommand(moveCommand)

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
                playScreen.moveCameraAction(unit.centerX, unit.centerY)

                queueCommand(command)
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
                return false
            } else {

                val moveCommand = MoveUnitCommand(unit, closestTileX, closestTileY)

                if (moveCommand.canExecute(playScreen)) {
                    playScreen.moveCameraAction(unit.centerX, unit.centerY)

                    queueCommand(moveCommand)

                    currentCommand = moveCommand
                    return true
                }
            }
        }

        return false
    }

    override fun onEnd() {
        super.onEnd()

        unitsAwaitingOrders.clear()
        enemyUnits.clear()
    }
}