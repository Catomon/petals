package ctmn.petals.editor

import com.badlogic.gdx.graphics.OrthographicCamera
import com.badlogic.gdx.graphics.g2d.Batch
import com.badlogic.gdx.graphics.g2d.Sprite
import ctmn.petals.utils.cornerX
import ctmn.petals.utils.cornerY

class TiledBackgroundDrawer(
    val sprite: Sprite,
    var tileWidth: Float = sprite.width,
    var tileHeight: Float = sprite.height,
) {

    fun draw(batch: Batch, camera: OrthographicCamera) {
        sprite.setSize(tileWidth * camera.zoom, tileHeight * camera.zoom)

        val spriteWidth = sprite.width
        val spriteHeight = sprite.height

        val x = (camera.viewportWidth * camera.zoom / spriteWidth).toInt()
        val y = (camera.viewportHeight * camera.zoom / spriteHeight).toInt()

        val cornerXMod = camera.cornerX() % spriteWidth
        val cornerYMod = camera.cornerY() % spriteHeight

        for (i in -1 until x + 2) {
            for (j in -1 until y + 2) {
                sprite.setPosition(
                    camera.cornerX() - cornerXMod + i * spriteWidth,
                    camera.cornerY() - cornerYMod + j * spriteHeight
                )
                sprite.draw(batch)
            }
        }
    }

    fun dispose() {
        sprite.texture.dispose()
    }
}