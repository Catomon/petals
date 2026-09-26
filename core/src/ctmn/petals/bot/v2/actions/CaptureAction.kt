package ctmn.petals.bot.v2.actions

import ctmn.petals.bot.v2.AiBot
import ctmn.petals.playscreen.commands.CaptureCommand
import ctmn.petals.playscreen.commands.MoveUnitCommand
import ctmn.petals.unit.UnitActor
import ctmn.petals.unit.actionPoints
import ctmn.petals.unit.canCapture
import ctmn.petals.unit.canMove
import ctmn.petals.tile.isCrystal
import ctmn.petals.bot.v2.util.Pathing
import ctmn.petals.unit.tiledX
import ctmn.petals.unit.tiledY

/** Captures a resource/base if the unit is on it, otherwise walks towards the closest one. */
class CaptureAction(bot: AiBot, private val unit: UnitActor) : BotAction(bot) {

    private var captureCommand: CaptureCommand? = null
    private var moveCommand: MoveUnitCommand? = null

    override fun evaluate(): Int {
        if (!bot.capabilities.canCapture) return IMPOSSIBLE
        if (!unit.canCapture()) return IMPOSSIBLE
        if (unit.actionPoints <= 0) return IMPOSSIBLE

        val tile = bot.field.getTile(unit.tiledX, unit.tiledY)
        if (tile != null && tile.isCrystal && unit.canCapture(tile)) {
            val cmd = CaptureCommand(unit, tile)
            if (cmd.canExecute(bot.battle)) {
                captureCommand = cmd
                return 80
            }
        }

        if (!unit.canMove()) return IMPOSSIBLE

        val target = bot.context.nearestCapturable(unit) ?: return IMPOSSIBLE
        val step = Pathing.stepTowards(bot.field, unit, target.tiledX, target.tiledY) ?: return IMPOSSIBLE
        val cmd = MoveUnitCommand(unit, step.tiledX, step.tiledY)
        if (!cmd.canExecute(bot.battle)) return IMPOSSIBLE

        moveCommand = cmd
        return 55
    }

    override fun execute(): Boolean {
        captureCommand?.let { return bot.battle.commandManager.queueCommand(it, bot.playerID) }
        moveCommand?.let { return bot.battle.commandManager.queueCommand(it, bot.playerID) }
        return false
    }

    override fun signature(): String = "capture:${unit.name}"

    override fun commandKind(): String = if (captureCommand != null) "capture" else "move"
}
