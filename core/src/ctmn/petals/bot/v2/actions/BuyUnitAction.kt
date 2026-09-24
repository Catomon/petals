package ctmn.petals.bot.v2.actions

import ctmn.petals.bot.v2.AiBot
import ctmn.petals.bot.v2.util.EconomyEval
import ctmn.petals.playscreen.commands.BuyUnitCommand
import ctmn.petals.playscreen.selfName
import ctmn.petals.tile.TileActor
import ctmn.petals.tile.isOccupied
import ctmn.petals.unit.cShop
import ctmn.petals.unit.isFollower

/** Buys the next unit at a free owned base. */
class BuyUnitAction(bot: AiBot, private val base: TileActor) : BotAction(bot) {

    private var command: BuyUnitCommand? = null

    override fun evaluate(): Int {
        if (!bot.capabilities.canBuy) return IMPOSSIBLE
        if (base.isOccupied) return IMPOSSIBLE

        val name = EconomyEval.chooseUnitToBuy(bot, bot.speciesUnits) ?: return IMPOSSIBLE
        val unit = bot.speciesUnits.firstOrNull { it.selfName == name } ?: return IMPOSSIBLE
        val price = unit.cShop?.price ?: return IMPOSSIBLE

        val leaderId = if (unit.isFollower) bot.context.leaderForNewUnit() else -1
        val cmd = BuyUnitCommand(name, bot.playerID, price, base.tiledX, base.tiledY, leaderId)
        if (!cmd.canExecute(bot.playScreen)) return IMPOSSIBLE

        command = cmd
        return 70
    }

    override fun execute(): Boolean {
        val cmd = command ?: return false
        return bot.playScreen.commandManager.queueCommand(cmd, bot.playerID)
    }

    override fun signature(): String = "buy@${base.tiledX},${base.tiledY}"
}
