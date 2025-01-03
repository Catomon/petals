package ctmn.petals.utils

import com.badlogic.gdx.graphics.Color
import com.badlogic.gdx.graphics.Pixmap

fun main() {
    println(packArgbToInt(255, 120, 215, 255))
}

fun packArgbToInt(alpha: Int, red: Int, green: Int, blue: Int): Int {
    return (alpha shl 24) or (red shl 16) or (green shl 8) or blue
}

fun Pixmap.replaceColor(replaceColor: Color, newColor: Color) {
    val pixmap = this

    pixmap.blending = Pixmap.Blending.None
    pixmap.filter = Pixmap.Filter.NearestNeighbour

    val newColorRgba8888 = Color.rgba8888(newColor)
    val pixelColor = Color()
    for (x in 0 until pixmap.width) {
        for (y in 0 until pixmap.height) {
            val pixel = pixmap.getPixel(x, y)
            Color.rgba8888ToColor(pixelColor, pixel)

            if (replaceColor == pixelColor)
                pixmap.drawPixel(x, y, newColorRgba8888)
        }
    }
}