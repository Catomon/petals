package ctmn.petals.bot.v2.actions

import ctmn.petals.bot.v2.AiBot
import ctmn.petals.bot.v2.util.CombatEval
import ctmn.petals.playscreen.commands.AttackCommand
import ctmn.petals.unit.UnitActor
import ctmn.petals.unit.canAttackNow

/** Attacks the best enemy target in range. Rewards kills and focus fire. */
class AttackAction(bot: AiBot, private val unit: UnitActor) : BotAction(bot) {

    private var command: AttackCommand? = null

    override fun evaluate(): Int {
        if (!unit.canAttackNow()) return IMPOSSIBLE

        val targets = bot.context.enemies.filter { unit.canAttackNow(it) }
        if (targets.isEmpty()) return IMPOSSIBLE

        val best = targets.maxByOrNull {
            CombatEval.attackScore(bot.playScreen, unit, it, bot.difficulty.skill)
        } ?: return IMPOSSIBLE

        val score = CombatEval.attackScore(bot.playScreen, unit, best, bot.difficulty.skill)
        val cmd = AttackCommand(unit, best)
        if (!cmd.canExecute(bot.playScreen)) return IMPOSSIBLE

        command = cmd
        return if (CombatEval.killsInOneHit(bot.playScreen, unit, best)) 95
        else 60 + (score / 10).coerceAtMost(20)
    }

    override fun execute(): Boolean {
        val cmd = command ?: return false
        return bot.playScreen.commandManager.queueCommand(cmd, bot.playerID)
    }

    override fun signature(): String = "attack:${unit.name}"
}
