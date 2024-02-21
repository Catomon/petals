package ctmn.petals.playscreen.commands

import ctmn.petals.Const
import ctmn.petals.playscreen.PlayScreen
import ctmn.petals.unit.*
import ctmn.petals.playscreen.queueAction
import ctmn.petals.playscreen.seqactions.MoveUnitAction
import ctmn.petals.playscreen.stageName
import ctmn.petals.unit.UnitActor

class MoveUnitCommand(val unitId: String, val tileX: Int, val tileY: Int) : Command() {

    constructor(unit: UnitActor, tileX: Int, tileY: Int) : this(unit.stageName, tileX, tileY)

    override fun canExecute(playScreen: PlayScreen): Boolean {
        val unit = playScreen.playStage.findUnit(unitId) ?: return false

        if (!unit.canMove(tileX, tileY)) return false

        //calculate AP
        return unit.actionPoints >= Const.ACTION_POINTS_MOVE_MIN
    }

    override fun execute(playScreen: PlayScreen): Boolean {
        val unit = playScreen.playStage.findUnit(unitId) ?: return false

        unit.actionPoints -= Const.ACTION_POINTS_MOVE

        //move action
        playScreen.queueAction(MoveUnitAction(unit, unit.tiledX, unit.tiledY, tileX, tileY))

        //change tiled position
        unit.tiledX = tileX
        unit.tiledY = tileY
        
        return true
    }
}
