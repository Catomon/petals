package ctmn.petals.bot.v2.actions

import ctmn.petals.bot.v2.AiBot
import ctmn.petals.bot.v2.BotLog
import ctmn.petals.bot.v2.util.ArmyPlanner
import ctmn.petals.playscreen.commands.BuyUnitCommand
import ctmn.petals.playscreen.selfName
import ctmn.petals.tile.TileActor
import ctmn.petals.tile.isOccupied
import ctmn.petals.unit.cShop
import ctmn.petals.unit.isFollower

/** Buys the next unit at a free owned base. */
class BuyUnitAction(bot: AiBot, private val base: TileActor) : BotAction(bot) {

    private var choice: ArmyPlanner.BuyChoice? = null
    private var price: Int = 0
    private var command: BuyUnitCommand? = null

    override fun evaluate(): Int {
        if (!bot.capabilities.canBuy) return IMPOSSIBLE
        if (base.isOccupied) return IMPOSSIBLE

        val selected = ArmyPlanner.chooseUnitToBuyChoice(bot, base, bot.speciesUnits) ?: return IMPOSSIBLE
        val unitPrice = selected.unit.cShop?.price ?: return IMPOSSIBLE

        val leaderId = if (selected.unit.isFollower) bot.context.leaderForNewUnit() else -1
        val cmd = BuyUnitCommand(selected.unit.selfName, bot.playerID, unitPrice, base.tiledX, base.tiledY, leaderId)
        if (!cmd.canExecute(bot.battle)) return IMPOSSIBLE

        choice = selected
        price = unitPrice
        command = cmd
        return 70
    }

    override fun execute(): Boolean {
        val cmd = command ?: return false
        val selected = choice ?: return false
        val queued = bot.battle.commandManager.queueCommand(cmd, bot.playerID)
        if (queued) BotLog.buy(bot, selected, base, price)
        return queued
    }

    override fun signature(): String = "buy@${base.tiledX},${base.tiledY}"
}
