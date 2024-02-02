package ctmn.petals.widgets

import com.badlogic.gdx.graphics.Texture
import ctmn.petals.utils.setPositionByCenter
import com.badlogic.gdx.graphics.g2d.Batch
import com.badlogic.gdx.graphics.g2d.Sprite
import com.badlogic.gdx.scenes.scene2d.Actor
import com.badlogic.gdx.scenes.scene2d.Group
import com.badlogic.gdx.utils.Array

class ParallaxBackground(val backgrounds: Array<MovingBackground>) : Group() {

    constructor(background1: MovingBackground, background2: MovingBackground, background3: MovingBackground? = null) : this(Array<MovingBackground>().apply { addAll(background1, background2); background3?.let { add(it) } })

    init {
        for (background in backgrounds) {
            addActor(background)
        }
    }
}

class MovingBackground(texture: Texture, var speed: Float = 0f) : Actor() {

    val sprite = Sprite(texture)
    private var offsetX = 0f

    override fun draw(batch: Batch?, parentAlpha: Float) {
        super.draw(batch, parentAlpha)

        sprite.setPositionByCenter(x + offsetX, y)
        sprite.draw(batch)

        sprite.setPositionByCenter(x + sprite.width + offsetX, y)
        sprite.draw(batch)
    }

    override fun act(delta: Float) {
        super.act(delta)

        offsetX -= speed * delta
        if (offsetX < -sprite.width)
            offsetX = 0f
    }

    override fun positionChanged() {
        super.positionChanged()

        sprite.setPositionByCenter(x + offsetX, y)
    }
}