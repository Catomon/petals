package ctmn.petals.bot.mediocre

import ctmn.petals.bot.BotAction
import ctmn.petals.playscreen.PlayScreen
import ctmn.petals.playscreen.commands.AttackCommand
import ctmn.petals.unit.UnitActor
import ctmn.petals.unit.canAttackNow
import ctmn.petals.utils.logErr

class AttackAction(val attackerUnit: UnitActor, private val bot: MidBot, val playScreen: PlayScreen) : BotAction() {
    var evaluated = false
        private set

    var defenderUnit: UnitActor? = null
        private set

    var attackCommand: AttackCommand? = null
        private set

    override val defaultPriority: Int = 50

    override fun evaluate(): Int {
        priority = defaultPriority

        if (bot.enemyUnits.isEmpty()) return IMPOSSIBLE

        defenderUnit =
            bot.enemyUnits.values.filter { attackerUnit.canAttackNow(it) }.randomOrNull() ?: return IMPOSSIBLE

        val atk = AttackCommand(attackerUnit, defenderUnit!!)
        if (!atk.canExecute(playScreen)) return IMPOSSIBLE

        attackCommand = atk

        evaluated = true
        priority = if (defenderUnit != null) defaultPriority else IMPOSSIBLE
        return priority
    }

    override fun execute(): Boolean {
        if (defenderUnit != null && attackCommand != null) {
            return playScreen.commandManager.queueCommand(attackCommand!!)
        } else {
            logErr("execute() failed. attacker: $attackerUnit. defender: $defenderUnit.")
        }

        return false
    }
}