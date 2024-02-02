package ctmn.petals.actors.actions

import com.badlogic.gdx.scenes.scene2d.Action
import kotlin.math.pow
import kotlin.math.sqrt

class MoveAction(
    private var startX: Float = 0f,
    private var startY: Float = 0f,
    private val endX: Float,
    private val endY: Float,
) : Action() {

    var time = 2f

    private var x = startX
    private var y = startY
    var vx = 0f
    var vy = 0f

    private var threshold = 1f

    init {
        vx = (endX - startX) / time
        vy = (endY - startY)  / time

        actor?.setPosition(startX, startY)
    }

    override fun act(delta: Float): Boolean {
        x += vx * delta
        y += vy * delta
        actor?.setPosition(x, y)

        if (sqrt((endX - x).pow(2) + (endY - y).pow(2)) < threshold ) {
            x = endX
            y = endY
            return true
        }

        return false
    }
}