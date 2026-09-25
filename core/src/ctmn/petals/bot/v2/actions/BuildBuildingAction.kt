package ctmn.petals.bot.v2.actions

import ctmn.petals.bot.v2.AiBot
import ctmn.petals.bot.v2.BotLog
import ctmn.petals.bot.v2.util.ArmyPlanner
import ctmn.petals.bot.v2.util.Pathing
import ctmn.petals.playscreen.commands.BuildCommand
import ctmn.petals.playscreen.commands.MoveUnitCommand
import ctmn.petals.player.Species
import ctmn.petals.playstage.getTiles
import ctmn.petals.playstage.tiledDst
import ctmn.petals.tile.TileActor
import ctmn.petals.tile.cPlayerId
import ctmn.petals.tile.components.BaseBuildingComponent
import ctmn.petals.tile.components.BuildingComponent
import ctmn.petals.tile.components.CapturingComponent
import ctmn.petals.tile.isPassableAndFree
import ctmn.petals.unit.UnitActor
import ctmn.petals.unit.actionPoints
import ctmn.petals.unit.canBuild
import ctmn.petals.unit.canMove
import ctmn.petals.unit.isInRange
import ctmn.petals.unit.tiledX
import ctmn.petals.unit.tiledY

/** Worker constructs economy buildings to unlock advanced units. */
class BuildBuildingAction(bot: AiBot, private val unit: UnitActor) : BotAction(bot) {

    private var buildCommand: BuildCommand? = null
    private var moveCommand: MoveUnitCommand? = null
    private var buildTile: TileActor? = null

    override fun evaluate(): Int {
        if (!bot.capabilities.canBuildBuildings) return IMPOSSIBLE
        if (!bot.difficulty.useBuildings) return IMPOSSIBLE
        if (!unit.canBuild()) return IMPOSSIBLE
        if (unit.actionPoints <= 0) return IMPOSSIBLE

        val building = ArmyPlanner.chooseBuilding(bot, bot.speciesUnits) ?: return IMPOSSIBLE
        if (bot.player.credits < building.cost) return IMPOSSIBLE

        val here = bot.playStage.getTile(unit.tiledX, unit.tiledY)
        if (here != null && canPlace(building, here)) {
            val cmd = BuildCommand(building.name, building.buildTime, building.cost, unit, here)
            if (cmd.canExecute(bot.playScreen)) {
                buildCommand = cmd
                buildTile = here
                return 68
            }
        }

        if (!unit.canMove()) return IMPOSSIBLE
        val site = findBuildSite(building) ?: return IMPOSSIBLE
        if (site.tiledX == unit.tiledX && site.tiledY == unit.tiledY) return IMPOSSIBLE

        val step = Pathing.stepTowards(bot.playStage, unit, site.tiledX, site.tiledY) ?: return IMPOSSIBLE
        val cmd = MoveUnitCommand(unit, step.tiledX, step.tiledY)
        if (!cmd.canExecute(bot.playScreen)) return IMPOSSIBLE

        moveCommand = cmd
        return 45
    }

    private fun canPlace(building: Species.Building, tile: TileActor): Boolean {
        if (tile.cPlayerId?.playerId == bot.playerID) return false
        if (!building.terrains.contains(tile.terrain, false)) return false
        if (tile.get(BuildingComponent::class.java) != null) return false
        if (tile.get(BaseBuildingComponent::class.java) != null) return false
        if (tile.get(CapturingComponent::class.java) != null) return false
        return true
    }

    private fun findBuildSite(building: Species.Building): TileActor? {
        val playStage = bot.playStage
        val enemies = bot.context.enemies
        val ownBase = bot.context.ownBases.firstOrNull()

        val buildable = playStage.getTiles().filter {
            it.isPassableAndFree() && canPlace(building, it)
        }
        if (buildable.isEmpty()) return null

        val safe = buildable.filter { tile -> enemies.none { it.isInRange(tile.tiledX, tile.tiledY, 2) } }
        val pool = safe.ifEmpty { buildable }

        val anchorX = ownBase?.tiledX ?: unit.tiledX
        val anchorY = ownBase?.tiledY ?: unit.tiledY
        return pool.minByOrNull { tiledDst(it.tiledX, it.tiledY, anchorX, anchorY) }
    }

    override fun execute(): Boolean {
        buildCommand?.let {
            val queued = bot.playScreen.commandManager.queueCommand(it, bot.playerID)
            val tile = buildTile
            if (queued && tile != null) BotLog.build(bot, it.buildingName, it.cost, tile)
            return queued
        }
        moveCommand?.let { return bot.playScreen.commandManager.queueCommand(it, bot.playerID) }
        return false
    }

    override fun signature(): String = "buildBuilding:${unit.name}"
}
