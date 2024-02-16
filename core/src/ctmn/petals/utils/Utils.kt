package ctmn.petals.utils

import com.badlogic.gdx.Gdx
import com.badlogic.gdx.graphics.Cursor
import com.badlogic.gdx.graphics.Pixmap
import com.badlogic.gdx.math.MathUtils

fun createCursor() {
    val pixmap = Pixmap(Gdx.files.internal("cursor.png"))
    val xHotspot = 0
    val yHotspot = 0
    val cursor: Cursor = Gdx.graphics.newCursor(pixmap, xHotspot, yHotspot)
    Gdx.graphics.setCursor(cursor)
    pixmap.dispose()
}

fun calculateMoveDirection(x: Float, y: Float, endX: Float, endY: Float): Float {
    val angleRad = MathUtils.atan2(endY - y, endX - x)
    val angleDeg = MathUtils.radiansToDegrees * angleRad
    return angleDeg
}

fun String.print() {
    println(this)
}