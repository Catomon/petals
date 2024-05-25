package ctmn.petals.actors.actions

import ctmn.petals.unit.UnitActor
import com.badlogic.gdx.scenes.scene2d.Action
import com.badlogic.gdx.scenes.scene2d.Actor
import ctmn.petals.playscreen.events.UnitMovedEvent
import ctmn.petals.playscreen.playStageOrNull
import ctmn.petals.utils.tiled

class JumpAction(
    var startX: Float = 0f,
    var startY: Float = 0f,
    var endX: Float,
    var endY: Float,
    val power: Float = 200f,
) : Action() {

    constructor(endX: Float, endY: Float, power: Float = 200f) : this(0f, 0f, endX, endY, power)

    private val gravity = -power / 10f * 20f

    private var x = startX
    private var y = startY
    private var vx = 0f
    private var vy = 0f

    private var currentTime = 0f

    private var movingRight = startX < endX
    private var sameX = x == endX

    init {
        setStart(startX, startY)
    }

    fun setStart(startX: Float, startY: Float) {
        x = startX
        y = startY

        movingRight = startX < endX
        sameX = x == endX

        // Calculate the initial velocities
        vx = (endX - startX)
        vy = (endY - startY) + power

        // Set the actor's initial position
        actor?.setPosition(startX, startY)

        if (actor is UnitActor) {
            val unit = actor as UnitActor
            unit.setAnimation(unit.airborneAnimation, 100f)
        }
    }

    override fun act(delta: Float): Boolean {

        currentTime += delta

        // Update the velocities
        vy += gravity * delta

        // Update the position
        x += vx * delta
        y += vy * delta
        actor?.setPosition(x, y)

        // Return true if the actor reaches the end position
        if ((sameX && (y <= endY && vy < 0)) || (!sameX && (movingRight && x > endX || !movingRight && x < endX))) {

            if (this.actor is UnitActor) {
                val unit = actor as UnitActor
                unit.setPosition(endX.tiled(), endY.tiled())
                unit.setAnimation(unit.postAirborneAnimation, 2.5f)

                unit.playStageOrNull?.root?.fire(UnitMovedEvent(unit, startX.tiled(), startY.tiled()))
            }
            else
                actor?.setPosition(endX, endY)

            return true
        }

        return false
    }

    override fun setActor(actor: Actor?) {
        super.setActor(actor)

        if (actor != null)
            setStart(actor.x, actor.y)
    }
}