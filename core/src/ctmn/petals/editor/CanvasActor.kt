package ctmn.petals.editor

import com.badlogic.gdx.graphics.g2d.Batch
import com.badlogic.gdx.graphics.g2d.Sprite
import com.badlogic.gdx.scenes.scene2d.Actor
import ctmn.petals.utils.setPositionByCenter

class CanvasActor(name: String, val sprite: Sprite = Sprite()) : Actor() {

    init {
        this.name = name

        setSize(tileSize, tileSize)

        if (sprite.height < tileSize && sprite.width < tileSize)
            sprite.setSize(tileSize, tileSize)

        debug = true
    }

    override fun draw(batch: Batch?, parentAlpha: Float) {
        super.draw(batch, parentAlpha)

        sprite.setPositionByCenter(x + tileSize / 2, y + tileSize / 2)
        sprite.draw(batch)
    }

    fun copy() : CanvasActor {
        val copy = CanvasActor(name)
        copy.sprite.set(sprite)
        copy.x = x
        copy.y = y
        copy.width = width
        copy.height = height
        copy.rotation = rotation
        copy.scaleX = scaleX
        copy.scaleY = scaleY
        copy.originX = originX
        copy.originY = originY
        copy.color.set(color)
        copy.isVisible = isVisible
        return copy
    }
}