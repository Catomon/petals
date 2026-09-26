package ctmn.petals.bot.v2.actions

import ctmn.petals.Config
import ctmn.petals.bot.v2.AiBot
import ctmn.petals.bot.v2.BotLog
import ctmn.petals.bot.v2.util.ArmyPlanner
import ctmn.petals.bot.v2.util.Pathing
import ctmn.petals.playscreen.commands.BuildBaseCommand
import ctmn.petals.playscreen.commands.MoveUnitCommand
import ctmn.petals.playstage.getTiles
import ctmn.petals.playstage.tiledDst
import ctmn.petals.tile.TerrainNames
import ctmn.petals.tile.TileActor
import ctmn.petals.tile.cPlayerId
import ctmn.petals.tile.isPassableAndFree
import ctmn.petals.unit.UnitActor
import ctmn.petals.unit.actionPoints
import ctmn.petals.unit.canBuildBase
import ctmn.petals.unit.canMove
import ctmn.petals.unit.isInRange
import ctmn.petals.unit.tiledX
import ctmn.petals.unit.tiledY

/** Worker builds a new base to grow income and unit production. */
class BuildBaseAction(bot: AiBot, private val unit: UnitActor) : BotAction(bot) {

    private var buildCommand: BuildBaseCommand? = null
    private var moveCommand: MoveUnitCommand? = null
    private var buildTile: TileActor? = null

    override fun evaluate(): Int {
        if (!bot.capabilities.canBuildBase) return IMPOSSIBLE
        if (!unit.canBuildBase()) return IMPOSSIBLE
        if (!ArmyPlanner.shouldBuildBase(bot)) return IMPOSSIBLE
        if (bot.player.credits < Config.BASE_BUILD_COST) return IMPOSSIBLE
        if (unit.actionPoints <= 0) return IMPOSSIBLE

        val here = bot.field.getTile(unit.tiledX, unit.tiledY)
        if (here != null && unit.canBuildBase(here) && here.cPlayerId?.playerId != bot.playerID) {
            val cmd = BuildBaseCommand(unit, here)
            if (cmd.canExecute(bot.battle)) {
                buildCommand = cmd
                buildTile = here
                return 78
            }
        }

        if (!unit.canMove()) return IMPOSSIBLE
        val site = findBaseSite() ?: return IMPOSSIBLE
        if (site.tiledX == unit.tiledX && site.tiledY == unit.tiledY) return IMPOSSIBLE

        val step = Pathing.stepTowards(bot.field, unit, site.tiledX, site.tiledY) ?: return IMPOSSIBLE
        val cmd = MoveUnitCommand(unit, step.tiledX, step.tiledY)
        if (!cmd.canExecute(bot.battle)) return IMPOSSIBLE

        moveCommand = cmd
        return 50
    }

    private fun findBaseSite(): TileActor? {
        val field = bot.field
        val enemies = bot.context.enemies

        val capturable = bot.context.nearestCapturable(unit)
        val ownBase = bot.context.ownBases.firstOrNull()
        val anchorX = capturable?.tiledX ?: ownBase?.tiledX ?: unit.tiledX
        val anchorY = capturable?.tiledY ?: ownBase?.tiledY ?: unit.tiledY

        val buildable = field.getTiles().filter {
            (it.terrain == TerrainNames.grass || it.terrain == TerrainNames.water) &&
                it.isPassableAndFree() &&
                it.cPlayerId?.playerId != bot.playerID
        }
        if (buildable.isEmpty()) return null

        val safe = buildable.filter { tile -> enemies.none { it.isInRange(tile.tiledX, tile.tiledY, 2) } }
        val pool = safe.ifEmpty { buildable }

        return pool.minByOrNull { tiledDst(it.tiledX, it.tiledY, anchorX, anchorY) }
    }

    override fun execute(): Boolean {
        buildCommand?.let {
            val queued = bot.battle.commandManager.queueCommand(it, bot.playerID)
            val tile = buildTile
            if (queued && tile != null) BotLog.buildBase(bot, Config.BASE_BUILD_COST, tile)
            return queued
        }
        moveCommand?.let { return bot.battle.commandManager.queueCommand(it, bot.playerID) }
        return false
    }

    override fun signature(): String = "buildBase:${unit.name}"

    override fun commandKind(): String = if (buildCommand != null) "buildBase" else "move"
}
