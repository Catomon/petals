package ctmn.petals.playscreen.seqactions

import com.badlogic.gdx.scenes.scene2d.actions.Actions
import com.badlogic.gdx.scenes.scene2d.actions.SequenceAction
import ctmn.petals.GameConst
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
        var distX = endX * GameConst.TILE_SIZE.toFloat() - unit.x
        var distY = endY * GameConst.TILE_SIZE.toFloat() - unit.y
        if (distX < 0)
            distX *= -1f
        if (distY < 0)
            distY *= -1f
        val moveActionX = Actions.moveTo(endX * GameConst.TILE_SIZE.toFloat(), unit.y,
            distX / GameConst.UNIT_MOVE_SPEED
        )
        val moveActionY = Actions.moveTo(endX * GameConst.TILE_SIZE.toFloat(),
            endY * GameConst.TILE_SIZE.toFloat(), distY / GameConst.UNIT_MOVE_SPEED
        )

        queAction.addAction(moveActionX)
        queAction.addAction(moveActionY)

        unit.addAction(queAction)

        return true
    }
}