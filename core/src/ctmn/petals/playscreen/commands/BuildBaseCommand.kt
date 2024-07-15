package ctmn.petals.playscreen.commands

import ctmn.petals.Const.ACTION_POINTS_ATTACK
import ctmn.petals.Const.BASE_BUILD_COST
import ctmn.petals.Const.BUILD_TIME
import ctmn.petals.playscreen.PlayScreen
import ctmn.petals.playscreen.stageName
import ctmn.petals.tile.TerrainNames
import ctmn.petals.tile.TileActor
import ctmn.petals.tile.cPlayerId
import ctmn.petals.tile.components.BaseBuildingComponent
import ctmn.petals.unit.UnitActor
import ctmn.petals.unit.actionPoints
import ctmn.petals.unit.canBuildBase
import ctmn.petals.unit.component.InvisibilityComponent
import ctmn.petals.unit.playerId
import ctmn.petals.utils.err

class BuildBaseCommand(val unitId: String, val baseId: String) : Command() {

    constructor(unit: UnitActor, base: TileActor) : this(unit.stageName, base.stageName)

    override fun canExecute(playScreen: PlayScreen): Boolean {
        val unit: UnitActor = playScreen.playStage.root.findActor(unitId)
        val tile: TileActor = playScreen.playStage.root.findActor(baseId)

        if (!unit.canBuildBase(tile)) return false

        if (unit.actionPoints <= 0) return false

        if ((playScreen.turnManager.getPlayerById(unit.playerId)?.credits ?: 0) < BASE_BUILD_COST) {
            err("Not enough credits")
            return false
        }

        return true
    }

    override fun execute(playScreen: PlayScreen): Boolean {
        val unit: UnitActor = playScreen.playStage.root.findActor(unitId)
        val tile: TileActor = playScreen.playStage.root.findActor(baseId)

        unit.actionPoints -= ACTION_POINTS_ATTACK

        playScreen.turnManager.getPlayerById(unit.playerId)!!.credits -= BASE_BUILD_COST

        unit.del(InvisibilityComponent::class.java)

        if (tile.cPlayerId?.playerId != unit.playerId) {
            tile.add(BaseBuildingComponent(unit.playerId, BUILD_TIME))
        }

        return true
    }
}