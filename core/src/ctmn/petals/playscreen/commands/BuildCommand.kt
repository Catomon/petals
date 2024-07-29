package ctmn.petals.playscreen.commands

import ctmn.petals.Const.ACTION_POINTS_ATTACK
import ctmn.petals.Const.BUILD_TIME
import ctmn.petals.playscreen.PlayScreen
import ctmn.petals.playscreen.stageName
import ctmn.petals.tile.TileActor
import ctmn.petals.tile.cPlayerId
import ctmn.petals.tile.components.BaseBuildingComponent
import ctmn.petals.tile.components.BuildingComponent
import ctmn.petals.unit.UnitActor
import ctmn.petals.unit.actionPoints
import ctmn.petals.unit.canBuild
import ctmn.petals.unit.component.InvisibilityComponent
import ctmn.petals.unit.playerId
import ctmn.petals.utils.err

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

    override fun canExecute(playScreen: PlayScreen): Boolean {
        val unit: UnitActor = playScreen.playStage.root.findActor(unitId)
        val tile: TileActor = playScreen.playStage.root.findActor(tileId)

        if (!unit.canBuild(tile)) return false

        if (unit.actionPoints <= 0) return false

        if ((playScreen.turnManager.getPlayerById(unit.playerId)?.credits ?: 0) < cost) {
            err("Not enough credits")
            return false
        }

        return true
    }

    override fun execute(playScreen: PlayScreen): Boolean {
        val unit: UnitActor = playScreen.playStage.root.findActor(unitId)
        val tile: TileActor = playScreen.playStage.root.findActor(tileId)

        unit.actionPoints = 0

        playScreen.turnManager.getPlayerById(unit.playerId)!!.credits -= cost

        unit.del(InvisibilityComponent::class.java)

        if (tile.cPlayerId?.playerId != unit.playerId) {
            tile.add(BuildingComponent(buildingName, playerId, buildTime))
        }

        return true
    }
}