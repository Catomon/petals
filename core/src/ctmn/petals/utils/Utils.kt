package ctmn.petals.utils

import com.badlogic.gdx.Gdx
import com.badlogic.gdx.graphics.Cursor
import com.badlogic.gdx.graphics.Pixmap
import com.badlogic.gdx.math.MathUtils


fun setMouseCursor(cursorFileName: String = "cursor.png") {
    Gdx.graphics.setCursor(createCursor(cursorFileName))
}

fun createCursor(cursorFileName: String = "cursor.png"): Cursor {
    val pixmap = Pixmap(Gdx.files.internal(cursorFileName))
    val xHotspot = 0
    val yHotspot = 0
    val cursor: Cursor = Gdx.graphics.newCursor(pixmap, xHotspot, yHotspot)
    pixmap.dispose()
    return cursor
}

fun calculateMoveDirection(x: Float, y: Float, endX: Float, endY: Float): Float {
    val angleRad = MathUtils.atan2(endY - y, endX - x)
    val angleDeg = MathUtils.radiansToDegrees * angleRad
    return angleDeg
}

fun String.print() {
    println(this)
}