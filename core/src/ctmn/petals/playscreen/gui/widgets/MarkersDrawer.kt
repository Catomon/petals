package ctmn.petals.playscreen.gui.widgets

import ctmn.petals.Const
import ctmn.petals.utils.RegionAnimation
import ctmn.petals.utils.setPositionByCenter
import ctmn.petals.utils.unTiled
import com.badlogic.gdx.graphics.g2d.Batch
import com.badlogic.gdx.scenes.scene2d.Actor
import com.badlogic.gdx.scenes.scene2d.EventListener
import com.badlogic.gdx.scenes.scene2d.Stage
import ctmn.petals.assets
import ctmn.petals.newPlaySprite
import ctmn.petals.playscreen.events.TaskBeginEvent
import ctmn.petals.playscreen.events.TaskCompletedEvent
import ctmn.petals.playscreen.tasks.MoveUnitTask

class MarkersDrawer : Actor() {

    private val waypointMark = assets.atlases.getRegion("gui/waypoint_mark")
    private val waypointAnimation = RegionAnimation(0.1f, assets.atlases.findRegions("gui/animated/waypoint_mark"))
    private val sprite = newPlaySprite(waypointMark)

    private val listener = EventListener {
        when (it) {
            is TaskBeginEvent -> {
                if (it.task is MoveUnitTask) {
                    showMarker(it.task.x, it.task.y)
                }
            }
            is TaskCompletedEvent -> {
                hideMarker()
            }
        }

        false
    }

    init {
        isVisible = false
    }

    override fun act(delta: Float) {
        super.act(delta)

        waypointAnimation.update(delta)
    }

    override fun draw(batch: Batch, parentAlpha: Float) {
        sprite.setRegion(waypointMark)
        sprite.draw(batch)

        sprite.setRegion(waypointAnimation.currentFrame)
        sprite.draw(batch)
    }

    fun showMarker(x: Int, y: Int) {
        setPosition(x.unTiled() + Const.TILE_SIZE / 2, y.unTiled() + Const.TILE_SIZE / 2)
        isVisible = true
    }

    fun hideMarker() {
        isVisible = false
    }

    override fun positionChanged() {
        super.positionChanged()

        sprite.setPositionByCenter(x + 0.5f, y - 0.5f)
    }

    override fun setStage(stage: Stage?) {
        super.setStage(stage)

        if (stage != null)
            stage.addListener(listener)
        else
            this.stage?.removeListener(listener)
    }
}