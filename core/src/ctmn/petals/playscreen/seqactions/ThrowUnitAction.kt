package ctmn.petals.playscreen.seqactions

import ctmn.petals.playscreen.PlayScreen
import ctmn.petals.tile.isPassable
import ctmn.petals.unit.UnitActor
import ctmn.petals.unit.tiledX
import ctmn.petals.unit.tiledY
import ctmn.petals.actors.actions.JumpAction
import ctmn.petals.utils.tiled
import ctmn.petals.utils.unTiled
import com.badlogic.gdx.utils.Array
import ctmn.petals.playscreen.events.UnitMovedEvent

class ThrowUnitAction(val unit: UnitActor, val dX: Int, val dY: Int, val power: Float = 100f) : SeqAction() {

    private val subActions = Array<ActorAction>()

    override fun update(deltaTime: Float) {
        for (action in subActions) {
            action.update(deltaTime)
            if (action.isDone) {
                subActions.removeValue(action, true)

                if (action.act is JumpAction) {
                    val act = action.act
                    if (act.actor is UnitActor) {
                        playScreen.fireEvent(UnitMovedEvent(act.actor as UnitActor, act.startX.tiled(), act.startY.tiled()))
                    }
                }
            }
        }

        if (subActions.isEmpty)
            isDone = true
    }

    override fun onStart(playScreen: PlayScreen): Boolean {
        var tileX = unit.tiledX
        var tileY = unit.tiledY

        val jumpActs = Array<JumpAction>()

        while (true) {
            val unit = playScreen.playStage.getUnit(tileX, tileY)

            tileX += dX
            tileY += dY

            if (unit != null) {
                val jumpAct = JumpAction(unit.x, unit.y, tileX.unTiled(), tileY.unTiled(), power)
                subActions.add(ActorAction(unit, jumpAct))

                jumpActs.add(jumpAct)
            } else
                break

            if (dX == 0 && dY == 0) break
        }

        var isBlocked = false
        jumpActs.forEach {
            if (playScreen.playStage.getTile(it.endX.tiled(), it.endY.tiled())?.isPassable() != true)
                isBlocked = true
        }

        if (isBlocked) {
            jumpActs.forEach {
                it.endX = it.startX
                it.endY = it.startY
            }
        }

        subActions.forEach {
            it.onStart(playScreen)
        }

        return true
    }
}