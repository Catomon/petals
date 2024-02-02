package ctmn.petals.utils

import com.badlogic.gdx.graphics.OrthographicCamera
import com.badlogic.gdx.graphics.g2d.Batch
import com.badlogic.gdx.graphics.g2d.Sprite

class TiledBackground(val sprite: Sprite) {

    fun draw(batch: Batch, camera: OrthographicCamera) {
        val x = (camera.viewportWidth * camera.zoom / sprite.width).toInt()
        val y = (camera.viewportHeight * camera.zoom / sprite.height).toInt()
        for (i in -1..x+1) {
            for (j in -1..y+1) {
                sprite.setPosition(
                    camera.cornerX() - (camera.cornerX() % sprite.width) + i * sprite.width,
                    camera.cornerY() - (camera.cornerY() % sprite.height) + j * sprite.height)
                sprite.draw(batch)
            }
        }
    }
}