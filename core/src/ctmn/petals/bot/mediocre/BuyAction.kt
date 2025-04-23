package ctmn.petals.bot.mediocre

import com.badlogic.gdx.utils.Array
import com.badlogic.gdx.utils.Logger
import ctmn.petals.bot.BotAction
import ctmn.petals.player.Species
import ctmn.petals.playscreen.PlayScreen
import ctmn.petals.playscreen.commands.BuyUnitCommand
import ctmn.petals.playscreen.selfName
import ctmn.petals.playstage.getTiles
import ctmn.petals.playstage.getUnitsOfPlayer
import ctmn.petals.tile.*
import ctmn.petals.unit.*
import ctmn.petals.utils.getUnitsInRange
import ctmn.petals.utils.logErr

class BuyAction(
    private val base: TileActor,
    private val bot: MidBot, val playScreen: PlayScreen,
    private val speciesUnits: Array<UnitActor>,
) : BotAction() {

    private val player = bot.player
    private val playerId = player.id

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

    var evaluated = false
        private set

    override val defaultPriority: Int = 50

    var buyCommand: BuyUnitCommand? = null
        private set

    private val log = Logger("BuyAction", Logger.DEBUG)

    init {

    }

    override fun evaluate(): Int {
        priority = defaultPriority

        if (bot.player.credits <= 0) {
            log.debug("evaluate() return IMPOSSIBLE; Not enough credits.")
            return IMPOSSIBLE
        }

        val playStage = playScreen.playStage

        val baseX = base.tiledX
        val baseY = base.tiledY

        val baseTile = playScreen.playStage.getTile(baseX, baseY) ?: return IMPOSSIBLE
        val isWater = baseTile.isWaterBase

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

        if (isWater && playScreen.playStage.getUnitsOfPlayer(playerId).filter { it.isWater }.size > 3) {
            log.debug("evaluate() return IMPOSSIBLE; The base in on the water and there can't be more than water units.")
            return IMPOSSIBLE
        }

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
            (bot.enemyUnits.values.count { it.isAir || it.selfName == UnitIds.DOLL_SCOUT || it.selfName == UnitIds.GOBLIN_WYVERN } * 0.65f).toInt()
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
        //                if (isWater) return IMPOSSIBLE
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
        if (player.credits < unitPrice) {
            log.debug("evaluate() return IMPOSSIBLE; Not enough credits.")
            return IMPOSSIBLE
        }

        //buy
        val buyCommand = BuyUnitCommand(unitToBuy, player.id, unitPrice, baseX, baseY)
        if (buyCommand.canExecute(playScreen)) {
            this.buyCommand = buyCommand

            log.debug("evaluate() return $priority")

            return priority
        }

        log.debug("evaluate() return IMPOSSIBLE; Nothing to buy i guess.")
        return IMPOSSIBLE
    }

    override fun execute(): Boolean {
        val buyCommand = buyCommand
        if (buyCommand != null) {
            return playScreen.commandManager.queueCommand(buyCommand)
        } else {
            logErr("execute() failed.")
        }

        return false
    }
}