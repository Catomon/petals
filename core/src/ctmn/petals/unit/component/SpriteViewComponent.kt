package ctmn.petals.unit.component

import com.badlogic.gdx.graphics.g2d.Sprite
import com.badlogic.gdx.graphics.g2d.SpriteBatch

open class SpriteViewComponent(
    val sprite: Sprite
) : ViewComponent() {

    override fun draw(batch: SpriteBatch) {
        sprite.draw(batch)
    }

    override fun setPosition(x: Float, y: Float) {
        sprite.setPosition(x - sprite.width / 2f, y - sprite.height / 2f)
    }
}