package ctmn.petals.widgets

import com.badlogic.gdx.graphics.Texture
import ctmn.petals.utils.setPositionByCenter
import com.badlogic.gdx.graphics.g2d.Batch
import com.badlogic.gdx.graphics.g2d.Sprite
import com.badlogic.gdx.scenes.scene2d.Actor
import com.badlogic.gdx.scenes.scene2d.Group
import com.badlogic.gdx.utils.Array

class ParallaxBackground(val backgrounds: Array<MovingBackground>) : Group() {

    constructor(
        background1: MovingBackground,
        background2: MovingBackground,
        background3: MovingBackground? = null,
    ) : this(Array<MovingBackground>().apply { addAll(background1, background2); background3?.let { add(it) } })

    init {
        for (background in backgrounds) {
            addActor(background)
        }
    }

    override fun setHeight(height: Float) {
        backgrounds.forEach { it.height = height }
    }
}

class MovingBackground(texture: Texture, var speed: Float = 0f) : Actor() {

    val sprite = Sprite(texture)
    private var offsetX = 0f

    override fun draw(batch: Batch, parentAlpha: Float) {
        super.draw(batch, parentAlpha)

        drawSprite(batch, x + offsetX, y)
        drawSprite(batch, x + sprite.width + offsetX, y)
    }

    private fun drawSprite(batch: Batch, x: Float, y: Float) {
        sprite.setPositionByCenter(x, y)
        sprite.draw(batch)
    }

    override fun act(delta: Float) {
        super.act(delta)

        offsetX -= speed * delta
        if (offsetX < -sprite.width)
            offsetX = 0f
    }

    override fun setHeight(height: Float) {
        val diff = height / sprite.height
        sprite.setSize((sprite.width * diff).toInt().toFloat(), (sprite.height * diff).toInt().toFloat())
    }
}