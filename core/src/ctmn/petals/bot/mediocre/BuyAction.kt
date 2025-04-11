package ctmn.petals.bot.mediocre

import ctmn.petals.bot.BotAction
import ctmn.petals.playscreen.PlayScreen
import ctmn.petals.playscreen.commands.AttackCommand
import ctmn.petals.playscreen.commands.BuyUnitCommand
import ctmn.petals.unit.UnitActor
import ctmn.petals.unit.canAttackNow
import ctmn.petals.utils.logErr

class BuyAction(private val bot: MidBot, val playScreen: PlayScreen) : BotAction() {
    var evaluated = false
        private set

    override val defaultPriority: Int = 50

    var buyCommand: BuyUnitCommand? = null
        private set

    override fun evaluate(): Int {
        if (bot.player.credits <= 0) return IMPOSSIBLE

        return priority
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