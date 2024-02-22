package ctmn.petals.editor

import com.badlogic.gdx.Gdx
import com.badlogic.gdx.graphics.g2d.SpriteBatch
import com.badlogic.gdx.graphics.glutils.ShapeRenderer
import com.badlogic.gdx.math.Rectangle
import com.badlogic.gdx.scenes.scene2d.Actor
import com.badlogic.gdx.scenes.scene2d.Group
import com.badlogic.gdx.scenes.scene2d.Stage
import com.badlogic.gdx.utils.Array
import com.badlogic.gdx.utils.viewport.ScreenViewport

class CanvasStage(
    screenViewport: ScreenViewport,
    batch: SpriteBatch,
    private val shapeRenderer: ShapeRenderer,
) : Stage(screenViewport, batch) {

    private val boundingRectangle = Rectangle(0f, 0f, tileSize, tileSize)
    private val updateBoundingRect = true

    override fun draw() {
        super.draw()

        shapeRenderer.projectionMatrix = camera.combined
        shapeRenderer.setAutoShapeType(true)
        shapeRenderer.begin()
        shapeRenderer.rect(boundingRectangle.x, boundingRectangle.y, boundingRectangle.width, boundingRectangle.height)

        for (canvasActor in getCanvasActors()) {
            with(canvasActor) {
                shapeRenderer.rect(x, y, originX, originY, width, height, scaleX, scaleY, rotation)
            }
        }
        shapeRenderer.end()
    }

    fun getCanvasActors(): Array<CanvasActor> {
        val canvasActors = Array<CanvasActor>()

        for (group in actors) {
            if (group is Group)
                for (actor in group.children) {
                    canvasActors.add(actor as CanvasActor)
                }
        }

        return canvasActors
    }

    fun addActor(actor: CanvasActor, layerId: Int) {
        findGroup(layerId).addActor(actor)

        updateBoundingRectangleSize()
    }

    fun removeActor(actor: CanvasActor) {
        actor.remove()

        updateBoundingRectangleSize()
    }

    /** Returns a Group with the specified layerId, creates a new one if it doesn't exist. */
    fun findGroup(layerId: Int): Group {
        // try to find existing
        val existingGroup = actors.find { it is Group && it.name == layerId.toString() } as? Group
        if (existingGroup != null) {
            return existingGroup
        }

        // crate new
        val newGroup = Group()
        newGroup.name = layerId.toString()
        root.addActor(newGroup)

        // sort all
        val groups = actors.filterIsInstance<Group>()
        groups.sortedBy { it.name.toInt() }
            .forEachIndexed { index, group -> group.zIndex = index }

        return newGroup
    }

    fun getLayers(): List<Group> {
        return actors.filterIsInstance<Group>()
    }

    override fun addActor(actor: Actor?) {
        Gdx.app.error(
            CanvasStage::class.simpleName,
            "You should use addActor(actor: Actor, layerId: Int) to add an actor to the CanvasStage."
        )
    }

    /** Changes layers visibility */
    fun changeLayersVisible(layerId: Int? = null) {
        if (layerId != null) {
            getLayers().forEach { it.isVisible = false }
            getLayers().firstOrNull { it.name == layerId.toString() }?.let {
                it.isVisible = true
            }
        } else {
            getLayers().forEach { it.isVisible = true }
        }
    }

    fun updateBoundingRectangleSize() {
        if (!updateBoundingRect)
            return

        boundingRectangle[0f, 0f, tileSize] = tileSize
        for (actor in getCanvasActors()) {
            var x = actor.x + actor.width
            var y = actor.y + actor.height

            if (actor.width == 0f)
                x += tileSize
            if (actor.height == 0f)
                y += tileSize

            if (x > boundingRectangle.width) boundingRectangle.width = x
            if (y > boundingRectangle.height) boundingRectangle.height = y
        }
    }
}