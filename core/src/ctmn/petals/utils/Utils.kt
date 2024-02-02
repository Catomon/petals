package ctmn.petals.utils

import com.badlogic.gdx.math.MathUtils

fun calculateMoveDirection(x: Float, y: Float, endX: Float, endY: Float): Float {
    val angleRad = MathUtils.atan2(endY - y, endX - x)
    val angleDeg = MathUtils.radiansToDegrees * angleRad
    return angleDeg
}