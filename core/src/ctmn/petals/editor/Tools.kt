package ctmn.petals.editor

import com.badlogic.gdx.graphics.OrthographicCamera
import com.badlogic.gdx.math.Vector2
import com.badlogic.gdx.scenes.scene2d.Event
import com.badlogic.gdx.scenes.scene2d.InputEvent
import com.badlogic.gdx.scenes.scene2d.InputListener

class Tools {

    val pencil = Pencil()
    val eraser = Eraser()
    val dragCanvas = DragCanvas()

    val toolList = listOf(
        pencil,
        eraser,
        dragCanvas
    )

    var current: Tool = pencil
        set(value) {
            field = value
            val canvas = this.canvas ?: return
            canvas.root?.listeners?.removeAll { it is Tool }
            canvas.addListener(value)
            toolChanged()
        }

    var canvas: CanvasStage? = null

    fun setContext(canvas: CanvasStage) {
        current = pencil
        this.canvas = canvas
        toolChanged()
    }

    private fun toolChanged() {
        val canvas = this.canvas!!
        current.setToolContext(this, canvas)
        canvas.root?.listeners?.removeAll { it is Tool }
        canvas.addListener(current)
    }
}

abstract class Tool(val name: String) : InputListener() {

    private val contextNotGivenException = "Use setToolContext(tools: Tools, canvas: CanvasStage) to initialize"

    private var _tools: Tools? = null
    private var _canvas: CanvasStage? = null

    protected val tools get() = _tools ?: throw IllegalStateException(contextNotGivenException)
    protected val canvas get() = _canvas ?: throw IllegalStateException(contextNotGivenException)

    protected val pointer1 = Vector2()
    protected val pointer2 = Vector2()

    protected val tempVector = Vector2()

    fun setToolContext(tools: Tools, canvas: CanvasStage) {
        _tools = tools
        _canvas = canvas
    }


    fun setPointer(x: Float, y: Float, pointer: Int) {
        if (pointer == 0)
            pointer1.set(x, y)
        else
            pointer2.set(x, y)
    }

    fun dragCanvas(x: Float, y: Float, pointer: Int = 1) {
        val pointerVec = if (pointer == 0) pointer1 else pointer2
        val deltaX = x - pointerVec.x
        val deltaY = y - pointerVec.y
        val camera = canvas.viewport.camera as OrthographicCamera
        camera.position.x -= deltaX
        camera.position.y -= deltaY
        camera.update()
    }

    override fun handle(e: Event?): Boolean {
        if (_canvas == null || _tools == null)
            throw IllegalStateException(contextNotGivenException)

        return super.handle(e)
    }
}

class Pencil : Tool("pencil") {

    var canvasActor: CanvasActor? = null

    var overlappingEnabled: Boolean = false
    var replacingEnabled: Boolean = false

    var isDrawing: Boolean = false

    var layer = 1

    override fun touchDown(event: InputEvent, x: Float, y: Float, pointer: Int, button: Int): Boolean {
        setPointer(x, y, button)

        if (button == 0) isDrawing = true

        return true
    }

    override fun touchUp(event: InputEvent, x: Float, y: Float, pointer: Int, button: Int) {
        if (button != 0) return
        else isDrawing = false

        draw(x, y)
    }

    override fun touchDragged(event: InputEvent, x: Float, y: Float, pointer: Int) {
        super.touchDragged(event, x, y, pointer)

        if (!isDrawing) {
            dragCanvas(x, y)

            return
        }

        if (!overlappingEnabled)
            draw(x, y)
    }

    fun draw(x: Float, y: Float) {
        if (x < 0 || y < 0) return

        val hitActor = canvas.getCanvasActors().firstOrNull {
            it.stageToLocalCoordinates(tempVector.set(x, y))
            it.hit(tempVector.x, tempVector.y, false) != null && it.layer == layer
        }

        if (hitActor is CanvasActor) {
            if (!overlappingEnabled && hitActor.layer == layer)
                return
            else
                if (replacingEnabled) hitActor.remove()
        }

        canvasActor?.let { canvasActor ->
            canvas.addActor(
                canvasActor.copy().apply {
                    setPosition(x - x % tileSize, y - y % tileSize)
                },
                layer
            )
        }
    }
}

class Eraser : Tool("eraser") {

    var isErasing = false

    var ignoreLayer = false

    override fun touchDown(event: InputEvent, x: Float, y: Float, pointer: Int, button: Int): Boolean {
        setPointer(x, y, button)

        if (button == 0) isErasing = true

        return true
    }

    override fun touchUp(event: InputEvent, x: Float, y: Float, pointer: Int, button: Int) {
        if (button != 0) return
        else isErasing = false

        delete(x, y)
    }

    override fun touchDragged(event: InputEvent, x: Float, y: Float, pointer: Int) {
        super.touchDragged(event, x, y, pointer)

        if (!isErasing) {
            dragCanvas(x, y)

            return
        }

        delete(x, y)
    }

    fun delete(x: Float, y: Float) {
        var actorHit: CanvasActor? = null
        for (actor in canvas.getCanvasActors()) {
            val localCoords = actor.stageToLocalCoordinates(Vector2(x, y))
            val isHit = actor.hit(localCoords.x, localCoords.y, false) != null
            val isSameLayer = actor.layer == tools.pencil.layer || ignoreLayer

            if (isHit && isSameLayer) {
                actorHit = actor
                break
            }
        }

        if (actorHit == null) return

        canvas.removeActor(actorHit)
    }
}

class Fill : Tool("fill") {


}

class Select : Tool("select") {

}

class DragCanvas : Tool("drag_canvas") {

    override fun touchDown(event: InputEvent?, x: Float, y: Float, pointer: Int, button: Int): Boolean {
        setPointer(x, y, pointer)

        return true
    }

    override fun touchDragged(event: InputEvent?, x: Float, y: Float, pointer: Int) {
        super.touchDragged(event, x, y, pointer)

        dragCanvas(x, y, pointer)
    }
}