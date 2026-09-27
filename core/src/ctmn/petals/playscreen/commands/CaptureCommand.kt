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
        // A legality check must never throw: on a client replaying the server's commands the
        // entities may simply not be there yet, and a throw freezes the whole command queue.
        val unit: UnitActor = battle.field.findUnit(unitId) ?: return false
        val base: TileActor = battle.field.allTiles.firstOrNull { it.name == baseId } ?: return false

        if (unit.actionPoints <= 0) return false

        if (!base.isCapturable)
            return false

        if (!unit.canCapture(base)) return false

        return !(unit.playerId == base.cPlayerId?.playerId ||
                unit.teamId == battle.turnManager.getPlayerById((base.cPlayerId?.playerId ?: -1))?.teamId)
    }

    override fun explainFailure(battle: Battle): String? {
        val unit = battle.field.findUnit(unitId)
            ?: return "no unit '$unitId' on the field"
        val base = battle.field.allTiles.firstOrNull { it.name == baseId }
            ?: return "no tile '$baseId' on the field"

        if (unit.actionPoints <= 0)
            return "unit $unitId has ${unit.actionPoints} action points"

        return "unit $unitId cannot capture tile '$baseId'"
    }

    override fun execute(battle: Battle): Boolean {
        val unit: UnitActor = battle.field.findUnit(unitId) ?: return false
        val base: TileActor = battle.field.allTiles.firstOrNull { it.name == baseId } ?: return false

        unit.actionPoints -= ACTION_POINTS_ATTACK

        unit.del(InvisibilityComponent::class.java)

        if (base.cPlayerId?.playerId != unit.playerId) {
            base.add(CapturingComponent(unit.playerId, CAPTURE_TIME))

            unit.updateView()
        }

        return true
    }
}
