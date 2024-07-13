package ctmn.petals.playscreen.commands

import ctmn.petals.Const
import ctmn.petals.playscreen.PlayScreen
import ctmn.petals.playscreen.queueAction
import ctmn.petals.playscreen.selfName
import ctmn.petals.playscreen.seqactions.MoveUnitAction
import ctmn.petals.playscreen.stageName
import ctmn.petals.tile.TileActor
import ctmn.petals.tile.TileData
import ctmn.petals.unit.*
import ctmn.petals.unit.component.MoveAfterAttackComponent

class MoveUnitCommand(val unitId: String, val tileX: Int, val tileY: Int) : Command() {

    constructor(unit: UnitActor, tileX: Int, tileY: Int) : this(unit.stageName, tileX, tileY)

    override fun canExecute(playScreen: PlayScreen): Boolean {
        val unit = playScreen.playStage.findUnit(unitId) ?: return false


        if (!unit.canMove(tileX, tileY)) return false

        //        val moveAfterAttackC = unit.get(MoveAfterAttackComponent::class.java)
        //        if (moveAfterAttackC?.attacked == true) {
        //            if (!unit.isInRange(tileX, tileY, moveAfterAttackC.range)) return false
        //        } else {

        //calculate AP
        return unit.actionPoints >= Const.ACTION_POINTS_MOVE_MIN
    }

    override fun execute(playScreen: PlayScreen): Boolean {
        val unit = playScreen.playStage.findUnit(unitId) ?: return false

        unit.actionPoints -= unit.actionPointsMove

        //move action
        playScreen.queueAction(MoveUnitAction(unit, unit.tiledX, unit.tiledY, tileX, tileY))

        if (unit.selfName == UnitIds.HUNTER || unit.selfName == UnitIds.GOBLIN_GIANT)
            playScreen.queueAction {
                playScreen.playStage.getTile(tileX, tileY)?.let {
                    if (it.terrain == "forest") {
                        it.remove()

                        playScreen.playStage.addActor(
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
                unit.actionPoints -= Const.ACTION_POINTS_ATTACK
                attacked = false
            }
        }

        //change tiled position
        unit.tiledX = tileX
        unit.tiledY = tileY

        return true
    }
}
