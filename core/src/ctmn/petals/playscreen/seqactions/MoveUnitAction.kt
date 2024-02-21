package ctmn.petals.playscreen.seqactions

import com.badlogic.gdx.scenes.scene2d.actions.Actions
import com.badlogic.gdx.scenes.scene2d.actions.SequenceAction
import ctmn.petals.Const
import ctmn.petals.playscreen.PlayScreen
import ctmn.petals.playscreen.events.UnitMovedEvent
import ctmn.petals.unit.UnitActor
import ctmn.petals.unit.tiledX
import ctmn.petals.unit.tiledY

class MoveUnitAction(val unit: UnitActor, val startX: Int, val startY: Int, val endX: Int, val endY: Int) : SeqAction() {

    private val queAction = SequenceAction()

    constructor(unit: UnitActor, endX: Int, endY: Int) : this(unit, unit.tiledX, unit.tiledY, endX, endY)

    override fun update(deltaTime: Float) {
        if (!unit.actions.contains(queAction)) {
            unit.setPosition(endX, endY)

            isDone = true

            playScreen.fireEvent(UnitMovedEvent(unit, startX, startY))
        }
    }

    override fun onStart(playScreen: PlayScreen): Boolean {
        var distX = endX * Const.TILE_SIZE.toFloat() - unit.x
        var distY = endY * Const.TILE_SIZE.toFloat() - unit.y
        if (distX < 0)
            distX *= -1f
        if (distY < 0)
            distY *= -1f
        val moveActionX = Actions.moveTo(endX * Const.TILE_SIZE.toFloat(), unit.y,
            distX / Const.UNIT_MOVE_SPEED
        )
        val moveActionY = Actions.moveTo(endX * Const.TILE_SIZE.toFloat(),
            endY * Const.TILE_SIZE.toFloat(), distY / Const.UNIT_MOVE_SPEED
        )

        queAction.addAction(moveActionX)
        queAction.addAction(moveActionY)

        unit.addAction(queAction)

        return true
    }
}