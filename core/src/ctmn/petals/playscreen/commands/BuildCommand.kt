package ctmn.petals.playscreen.commands

import ctmn.petals.playscreen.stageName
import ctmn.petals.sim.Battle
import ctmn.petals.tile.TileActor
import ctmn.petals.tile.cPlayerId
import ctmn.petals.tile.components.BuildingComponent
import ctmn.petals.unit.UnitActor
import ctmn.petals.unit.actionPoints
import ctmn.petals.unit.canBuild
import ctmn.petals.unit.component.InvisibilityComponent
import ctmn.petals.unit.playerId
import ctmn.petals.utils.logErr

class BuildCommand(
    val buildingName: String,
    val buildTime: Int,
    val cost: Int,
    val unitId: String,
    val tileId: String,
) : Command() {

    constructor(buildingName: String, buildTime: Int, cost: Int, unit: UnitActor, tile: TileActor) : this(
        buildingName,
        buildTime,
        cost,
        unit.stageName,
        tile.stageName
    )

    override fun canExecute(battle: Battle): Boolean {
        val unit: UnitActor = battle.field.findUnit(unitId)!!
        val tile: TileActor = battle.field.allTiles.firstOrNull { it.name == tileId }!!

        if (!unit.canBuild(tile)) return false

        if (unit.actionPoints <= 0) return false

        if ((battle.turnManager.getPlayerById(unit.playerId)?.credits ?: 0) < cost) {
            logErr("Not enough credits")
            return false
        }

        return true
    }

    override fun execute(battle: Battle): Boolean {
        val unit: UnitActor = battle.field.findUnit(unitId)!!
        val tile: TileActor = battle.field.allTiles.firstOrNull { it.name == tileId }!!

        unit.actionPoints = 0

        battle.turnManager.getPlayerById(unit.playerId)!!.credits -= cost

        unit.del(InvisibilityComponent::class.java)

        if (tile.cPlayerId?.playerId != unit.playerId) {
            tile.add(BuildingComponent(buildingName, playerId, buildTime))
        }

        unit.updateView()

        return true
    }
}
