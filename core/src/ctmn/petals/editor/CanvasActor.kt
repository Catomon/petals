package ctmn.petals.editor

import com.badlogic.gdx.graphics.g2d.Batch
import com.badlogic.gdx.graphics.g2d.Sprite
import com.badlogic.gdx.scenes.scene2d.Actor
import com.badlogic.gdx.scenes.scene2d.Group
import com.badlogic.gdx.scenes.scene2d.Stage
import ctmn.petals.utils.setPositionByCenter

class CanvasActor(name: String, val sprite: Sprite = Sprite()) : Actor() {

    val canvasStage: CanvasStage? get() = stage as CanvasStage?

    var layer = 1

    var favouriteLayer: Int? = null

    init {
        this.name = name

        setSize(tileSize, tileSize)

        if (sprite.height < tileSize && sprite.width < tileSize)
            sprite.setSize(tileSize, tileSize)
    }

    override fun draw(batch: Batch, parentAlpha: Float) {
        if (!isVisible) return

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

    /** See [CanvasStage.findGroup]*/
    override fun setParent(parent: Group?) {
        super.setParent(parent)

        if (parent != null) {
            layer = parent.name.toIntOrNull() ?: 1
        }
    }

    override fun setStage(stage: Stage?) {
        super.setStage(stage)

        canvasStage?.contentChanged = true
    }

    override fun positionChanged() {
        super.positionChanged()

        canvasStage?.contentChanged = true
    }

    override fun sizeChanged() {
        super.sizeChanged()

        canvasStage?.contentChanged = true
    }

    override fun scaleChanged() {
        super.scaleChanged()

        canvasStage?.contentChanged = true
    }

    override fun rotationChanged() {
        super.rotationChanged()

        canvasStage?.contentChanged = true
    }
}