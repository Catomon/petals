package ctmn.petals.playscreen.commands

import ctmn.petals.Config
import ctmn.petals.playstage.getUnits
import ctmn.petals.playscreen.selfName
import ctmn.petals.playscreen.seqactions.MoveUnitAction
import ctmn.petals.playscreen.seqactions.OneSeqAction
import ctmn.petals.playscreen.stageName
import ctmn.petals.sim.Battle
import ctmn.petals.tile.TileActor
import ctmn.petals.tile.TileData
import ctmn.petals.unit.*
import ctmn.petals.unit.component.MoveAfterAttackComponent

class MoveUnitCommand(val unitId: String, val tileX: Int, val tileY: Int) : Command() {

    constructor(unit: UnitActor, tileX: Int, tileY: Int) : this(unit.stageName, tileX, tileY)

    override fun canExecute(battle: Battle): Boolean {
        val unit = battle.field.findUnit(unitId) ?: return false


        if (!unit.canMove(tileX, tileY)) return false

        //        val moveAfterAttackC = unit.get(MoveAfterAttackComponent::class.java)
        //        if (moveAfterAttackC?.attacked == true) {
        //            if (!unit.isInRange(tileX, tileY, moveAfterAttackC.range)) return false
        //        } else {

        //calculate AP
        return unit.actionPoints >= Config.ACTION_POINTS_MOVE_MIN
    }

    override fun explainFailure(battle: Battle): String? {
        val unit = battle.field.findUnit(unitId)
            ?: return "no unit '$unitId' on the field (has ${battle.field.getUnits().size} unit(s))"

        if (!unit.canMove(tileX, tileY)) {
            return "unit $unitId at ${unit.tiledX},${unit.tiledY} cannot reach $tileX,$tileY " +
                    "(move range ${unit.movingRange}, action points ${unit.actionPoints})"
        }

        if (unit.actionPoints < Config.ACTION_POINTS_MOVE_MIN) {
            return "unit $unitId has ${unit.actionPoints} action points, needs " +
                    "${Config.ACTION_POINTS_MOVE_MIN}"
        }

        return null
    }

    override fun execute(battle: Battle): Boolean {
        val unit = battle.field.findUnit(unitId) ?: return false

        val startX = unit.tiledX
        val startY = unit.tiledY

        unit.actionPoints -= unit.actionPointsMove

        // Workers that destroy forest on the way apply it immediately, not after the animation.
        if (unit.selfName == UnitIds.HUNTER || unit.selfName == UnitIds.GOBLIN_GIANT) {
            battle.field.getTile(tileX, tileY)?.let {
                if (it.terrain == "forest") {
                    battle.field.removeGameActor(it)
                    battle.field.addGameActor(
                        TileActor(
                            TileData.get("fallen_forest")!!,
                            it.layer,
                            it.tiledX,
                            it.tiledY
                        )
                    )
                }
            }
        }

        unit.get(MoveAfterAttackComponent::class.java)?.apply {
            if (attacked) {
                unit.cUnit.movingRange = normalRange
                unit.actionPoints -= Config.ACTION_POINTS_ATTACK
                attacked = false
            }
        }

        //change tiled position
        unit.tiledX = tileX
        unit.tiledY = tileY

        // Terrain damage, item pickups and similar rules react to this event.
        battle.fireEvent(ctmn.petals.playscreen.events.UnitMovedEvent(unit, startX, startY))

        //move animation
        battle.queueSeqAction(MoveUnitAction(unit, startX, startY, tileX, tileY))

        return true
    }
}
