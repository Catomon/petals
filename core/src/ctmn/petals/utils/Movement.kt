package ctmn.petals.utils

import com.badlogic.gdx.math.Vector2
import kotlin.math.cos
import kotlin.math.sin

class Movement {

    val velocity = Vector2()

    val acceleration = Vector2()
    val deceleration = Vector2()
    val direction = Vector2(1f, 1f)
    val maxVelocity = Vector2()
    val decelerationLimit = Vector2()

    val dirVelocity = Vector2()

    fun update(delta: Float) {
        if (velocity.x > decelerationLimit.x) {
            velocity.x -= deceleration.x * delta
            if (velocity.x < decelerationLimit.x)
                velocity.x = decelerationLimit.x
        }
        else
            if (velocity.x < decelerationLimit.x) {
                velocity.x += deceleration.x * delta
                if (velocity.x > decelerationLimit.x)
                    velocity.x = decelerationLimit.x
            }

        if (velocity.y > decelerationLimit.y) {
            velocity.y -= deceleration.y * delta
            if (velocity.y < decelerationLimit.y)
                velocity.y = decelerationLimit.y
        }
        else
            if (velocity.y < decelerationLimit.y) {
                velocity.y += deceleration.y * delta
                if (velocity.y > decelerationLimit.y)
                    velocity.y = decelerationLimit.y
            }

        velocity.y += acceleration.y * delta
        velocity.x += acceleration.x * delta

        velocity.x = velocity.x.coerceIn(-maxVelocity.x, maxVelocity.x)
        velocity.y = velocity.y.coerceIn(-maxVelocity.y, maxVelocity.y)

        dirVelocity.set(velocity.x * direction.x * delta, velocity.y * direction.y * delta)
    }

    fun setDirection(degree: Double) {
        direction.x = cos(Math.toRadians(degree)).toFloat()
        direction.y = sin(Math.toRadians(degree)).toFloat()
    }

    fun setDirection(x: Float, y: Float, dirX: Float, dirY: Float) {
        direction.x = (dirX - x) / Vector2.dst(x, y, dirX, dirY)
        direction.y = (dirY - y) / Vector2.dst(x, y, dirX, dirY)
    }
}