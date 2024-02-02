package ctmn.petals.unit.component

import com.badlogic.gdx.graphics.g2d.SpriteBatch
import com.badlogic.ashley.core.Component

abstract class ViewComponent : Component {

    var flipToEnemy = true

    var flipX = false
    var flipY = false

    open fun update(delta: Float) {

    }

    abstract fun draw(batch: SpriteBatch)

    abstract fun setPosition(x: Float, y: Float)
}