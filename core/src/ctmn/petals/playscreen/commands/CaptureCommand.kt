package ctmn.petals.playscreen.commands

import ctmn.petals.Const.ACTION_POINTS_ATTACK
import ctmn.petals.Const.CAPTURE_TIME
import ctmn.petals.playscreen.PlayScreen
import ctmn.petals.playscreen.stageName
import ctmn.petals.tile.*
import ctmn.petals.tile.components.CapturingComponent
import ctmn.petals.unit.*
import ctmn.petals.unit.component.InvisibilityComponent

class CaptureCommand(val unitId: String, val baseId: String) : Command() {

    constructor(unit: UnitActor, base: TileActor) : this(unit.stageName, base.stageName)

    override fun canExecute(playScreen: PlayScreen): Boolean {
        val unit: UnitActor = playScreen.playStage.root.findActor(unitId)
        val base: TileActor = playScreen.playStage.root.findActor(baseId)

        if (unit.actionPoints <= 0) return false

        if (!base.isCapturable)
            throw IllegalArgumentException("The tile terrain is not capturable")

        if (!unit.canCapture(base)) return false

        return !(unit.playerId == base.cPlayerId?.playerId ||
                unit.teamId == playScreen.turnManager.getPlayerById((base.cPlayerId?.playerId ?: -1))?.teamId)
    }

    override fun execute(playScreen: PlayScreen): Boolean {
        val unit: UnitActor = playScreen.playStage.root.findActor(unitId)
        val base: TileActor = playScreen.playStage.root.findActor(baseId)

        unit.actionPoints -= ACTION_POINTS_ATTACK

        unit.del(InvisibilityComponent::class.java)

        if (base.cPlayerId?.playerId != unit.playerId) {
            base.add(CapturingComponent(unit.playerId, CAPTURE_TIME))
        }

        return true
    }
}