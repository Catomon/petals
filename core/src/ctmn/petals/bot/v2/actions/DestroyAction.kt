package ctmn.petals.bot.v2.actions

import ctmn.petals.bot.v2.AiBot
import ctmn.petals.bot.v2.util.Pathing
import ctmn.petals.playscreen.commands.DestroyTileCommand
import ctmn.petals.playscreen.commands.MoveUnitCommand
import ctmn.petals.tile.cPlayerId
import ctmn.petals.unit.UnitActor
import ctmn.petals.unit.actionPoints
import ctmn.petals.unit.canDestroy
import ctmn.petals.unit.canMove
import ctmn.petals.unit.tiledX
import ctmn.petals.unit.tiledY

/** Combat units raze enemy bases and buildings. */
class DestroyAction(bot: AiBot, private val unit: UnitActor) : BotAction(bot) {

    private var destroyCommand: DestroyTileCommand? = null
    private var moveCommand: MoveUnitCommand? = null

    override fun evaluate(): Int {
        if (!bot.capabilities.canDestroy) return IMPOSSIBLE
        if (!unit.canDestroy()) return IMPOSSIBLE
        if (unit.actionPoints <= 0) return IMPOSSIBLE

        val here = bot.field.getTile(unit.tiledX, unit.tiledY)
        if (here != null && here.cPlayerId?.playerId != null) {
            val teamId = bot.battle.turnManager.getPlayerById(here.cPlayerId?.playerId ?: -1)?.teamId
            if (unit.canDestroy(here, teamId)) {
                val cmd = DestroyTileCommand(unit, here)
                if (cmd.canExecute(bot.battle)) {
                    destroyCommand = cmd
                    return 72
                }
            }
        }

        if (!unit.canMove()) return IMPOSSIBLE
        val target = bot.context.nearestEnemyBuilding(unit) ?: return IMPOSSIBLE
        val step = Pathing.stepTowards(bot.field, unit, target.tiledX, target.tiledY) ?: return IMPOSSIBLE
        val cmd = MoveUnitCommand(unit, step.tiledX, step.tiledY)
        if (!cmd.canExecute(bot.battle)) return IMPOSSIBLE

        moveCommand = cmd
        return 44
    }

    override fun execute(): Boolean {
        destroyCommand?.let { return bot.battle.commandManager.queueCommand(it, bot.playerID) }
        moveCommand?.let { return bot.battle.commandManager.queueCommand(it, bot.playerID) }
        return false
    }

    override fun signature(): String = "destroy:${unit.name}"

    override fun commandKind(): String = if (destroyCommand != null) "destroy" else "move"
}
