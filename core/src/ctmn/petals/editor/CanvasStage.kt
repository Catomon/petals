package ctmn.petals.editor

import com.badlogic.gdx.Gdx
import com.badlogic.gdx.graphics.Color
import com.badlogic.gdx.graphics.GL32
import com.badlogic.gdx.graphics.g2d.SpriteBatch
import com.badlogic.gdx.graphics.glutils.ShapeRenderer
import com.badlogic.gdx.math.Rectangle
import com.badlogic.gdx.scenes.scene2d.Actor
import com.badlogic.gdx.scenes.scene2d.Group
import com.badlogic.gdx.scenes.scene2d.Stage
import com.badlogic.gdx.utils.Array
import com.badlogic.gdx.utils.viewport.ScreenViewport
import com.kotcrab.vis.ui.widget.VisLabel

/** Contains Groups that represent layers; layerId can be obtained using <group>.name.toInt() */
class CanvasStage(
    screenViewport: ScreenViewport,
    batch: SpriteBatch,
    private val shapeRenderer: ShapeRenderer,
) : Stage(screenViewport, batch) {

    private val boundingRectangle = Rectangle(0f, 0f, tileSize, tileSize)
    private val updateBoundingRect = true

    private var highlightLayer: Group? = null
    var highlightLayerId: Int? = null
        set(value) {
            field = value
            highlightLayer = if (value == null) null else getLayer(value)
        }

    private val outlineColor = Color.WHITE.cpy().apply { a = 0.5f }
    private val outlineColorHighlighted = Color.GREEN.cpy().apply { a = 0.5f }

    private val sizeLabel = VisLabel("0")

    var contentChanged = false

    override fun draw() {
        super.draw()

        Gdx.gl.glEnable(GL32.GL_BLEND);
        Gdx.gl.glBlendFunc(GL32.GL_SRC_ALPHA, GL32.GL_ONE_MINUS_SRC_ALPHA);
        shapeRenderer.projectionMatrix = camera.combined
        shapeRenderer.setAutoShapeType(true)

        shapeRenderer.begin()
        shapeRenderer.color = outlineColor
        shapeRenderer.rect(boundingRectangle.x, boundingRectangle.y, boundingRectangle.width, boundingRectangle.height)

        // if highlightLayerId was set before a group was created
        if (highlightLayer == null && highlightLayerId != null) highlightLayer = getLayer(highlightLayerId!!)

        val groups =
            if (highlightLayer != null) getLayers().filter { it.name != highlightLayer!!.name } else getLayers()
        for (group in groups) {
            drawShapes(group)
        }

        if (highlightLayer != null) {
            shapeRenderer.color = outlineColorHighlighted
            drawShapes(highlightLayer!!)
        }

        shapeRenderer.end()
        Gdx.gl.glDisable(GL32.GL_BLEND);

        batch.begin()
        sizeLabel.setText((boundingRectangle.height / tileSize).toInt())
        sizeLabel.setPosition(0f - tileSize / 2, 0f + tileSize / 2)
        sizeLabel.draw(batch, root.color.a)

        sizeLabel.setText((boundingRectangle.width / tileSize).toInt())
        sizeLabel.setPosition(0f + tileSize / 2, 0f - tileSize / 2)
        sizeLabel.draw(batch, root.color.a)
        batch.end()
    }

    private fun drawShapes(group: Group) {
        for (canvasActor in group.children) {
            with(canvasActor) {
                shapeRenderer.rect(x, y, originX, originY, width, height, scaleX, scaleY, rotation)
            }
        }
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
    }

    override fun actorRemoved(actor: Actor?) {
        super.actorRemoved(actor)

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

    fun getLayer(layerId: Int): Group? {
        return getLayers().firstOrNull { it.name.toInt() == layerId }
    }

    fun getActor(x: Int, y: Int, layer: Int): CanvasActor? =
        getCanvasActors().find { it.x.toTilePos() == x && it.y.toTilePos() == y && it.layer == layer }

    @Deprecated(
        "You should use addActor(actor: Actor, layerId: Int) to add an actor to the CanvasStage.",
        ReplaceWith("addActor(actor: Actor, layerId: Int)"),
        DeprecationLevel.ERROR
    )
    override fun addActor(actor: Actor?) {
        Gdx.app.error(
            CanvasStage::class.simpleName,
            "You should use addActor(actor: Actor, layerId: Int) to add an actor to the CanvasStage."
        )
    }

    fun isEmpty() = getCanvasActors().isEmpty

    fun clearCanvasActors() {
        getCanvasActors().forEach { it.remove() }
        updateBoundingRectangleSize()
        contentChanged = true
    }

    /** Changes layers visibility.
     * @param layerId If == null, all visible */
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