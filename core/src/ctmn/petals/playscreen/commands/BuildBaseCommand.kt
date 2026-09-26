package ctmn.petals.playscreen.commands

import ctmn.petals.Config.BASE_BUILD_COST
import ctmn.petals.Config.BASE_BUILD_TIME
import ctmn.petals.playscreen.stageName
import ctmn.petals.sim.Battle
import ctmn.petals.tile.TileActor
import ctmn.petals.tile.cPlayerId
import ctmn.petals.tile.components.BaseBuildingComponent
import ctmn.petals.unit.UnitActor
import ctmn.petals.unit.actionPoints
import ctmn.petals.unit.canBuildBase
import ctmn.petals.unit.component.InvisibilityComponent
import ctmn.petals.unit.playerId
import ctmn.petals.utils.logErr

class BuildBaseCommand(val unitId: String, val baseId: String) : Command() {

    constructor(unit: UnitActor, base: TileActor) : this(unit.stageName, base.stageName)

    override fun canExecute(battle: Battle): Boolean {
        val unit: UnitActor = battle.field.findUnit(unitId)!!
        val tile: TileActor = battle.field.allTiles.firstOrNull { it.name == baseId }!!

        if (!unit.canBuildBase(tile)) return false

        if (unit.actionPoints <= 0) return false

        if ((battle.turnManager.getPlayerById(unit.playerId)?.credits ?: 0) < BASE_BUILD_COST) {
            logErr("Not enough credits")
            return false
        }

        return true
    }

    override fun execute(battle: Battle): Boolean {
        val unit: UnitActor = battle.field.findUnit(unitId)!!
        val tile: TileActor = battle.field.allTiles.firstOrNull { it.name == baseId }!!

        unit.actionPoints = 0

        battle.turnManager.getPlayerById(unit.playerId)!!.credits -= BASE_BUILD_COST

        unit.del(InvisibilityComponent::class.java)

        if (tile.cPlayerId?.playerId != unit.playerId) {
            tile.add(BaseBuildingComponent(unit.playerId, BASE_BUILD_TIME))

            unit.updateView()
        }

        return true
    }
}
