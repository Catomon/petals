package ctmn.petals.playscreen.commands

import ctmn.petals.Config.ACTION_POINTS_ATTACK
import ctmn.petals.Config.CAPTURE_TIME
import ctmn.petals.playscreen.stageName
import ctmn.petals.sim.Battle
import ctmn.petals.tile.*
import ctmn.petals.tile.components.CapturingComponent
import ctmn.petals.unit.*
import ctmn.petals.unit.component.InvisibilityComponent

class CaptureCommand(val unitId: String, val baseId: String) : Command() {

    constructor(unit: UnitActor, base: TileActor) : this(unit.stageName, base.stageName)

    override fun canExecute(battle: Battle): Boolean {
        val unit: UnitActor = battle.field.findUnit(unitId)!!
        val base: TileActor = battle.field.allTiles.firstOrNull { it.name == baseId }!!

        if (unit.actionPoints <= 0) return false

        if (!base.isCapturable)
            throw IllegalArgumentException("The tile terrain is not capturable")

        if (!unit.canCapture(base)) return false

        return !(unit.playerId == base.cPlayerId?.playerId ||
                unit.teamId == battle.turnManager.getPlayerById((base.cPlayerId?.playerId ?: -1))?.teamId)
    }

    override fun execute(battle: Battle): Boolean {
        val unit: UnitActor = battle.field.findUnit(unitId)!!
        val base: TileActor = battle.field.allTiles.firstOrNull { it.name == baseId }!!

        unit.actionPoints -= ACTION_POINTS_ATTACK

        unit.del(InvisibilityComponent::class.java)

        if (base.cPlayerId?.playerId != unit.playerId) {
            base.add(CapturingComponent(unit.playerId, CAPTURE_TIME))

            unit.updateView()
        }

        return true
    }
}
