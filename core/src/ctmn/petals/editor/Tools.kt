package ctmn.petals.editor

import com.badlogic.gdx.graphics.OrthographicCamera
import com.badlogic.gdx.math.Vector2
import com.badlogic.gdx.scenes.scene2d.InputEvent
import com.badlogic.gdx.scenes.scene2d.InputListener
import com.badlogic.gdx.scenes.scene2d.Stage

sealed class Tool : InputListener() {

    @Suppress("GDXKotlinStaticResource")
    companion object {

        var current: Tool = Pencil
            set(value) {
                field = value
                _canvas?.root?.listeners?.removeAll { it is Tool }
                _canvas?.addListener(value)
                value.toolChanged()
            }

        private var _canvas: Stage? = null
        val canvas get() = _canvas ?: throw IllegalStateException("canvas is null")

        fun setCanvas(canvas: Stage) {
            _canvas = canvas
            _canvas?.root?.listeners?.removeAll { it is Tool }
            _canvas?.addListener(current)
            current.toolChanged()
        }

        private val pointer1 = Vector2()
        private val pointer2 = Vector2()

        private fun setPointer(x: Float, y: Float, pointer: Int) {
            if (pointer == 0)
                pointer1.set(x, y)
            else
                pointer2.set(x, y)
        }

        private fun dragCanvas(x: Float, y: Float) {
            val deltaX = x - pointer2.x
            val deltaY = y - pointer2.y
            val camera = canvas.viewport.camera as OrthographicCamera
            camera.position.x -= deltaX
            camera.position.y -= deltaY
            camera.update()
        }
    }

    open fun toolChanged() {

    }

    data object Pencil : Tool() {

        var canvasActor: CanvasActor? = null

        var overlappingEnabled: Boolean = false
        var replacingEnabled: Boolean = false

        var isDrawing: Boolean = false

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

            val hitActor = canvas.hit(x, y, false)
            if (!overlappingEnabled && hitActor != null)
                return
            else
                if (replacingEnabled && hitActor != null) hitActor.remove()

            canvasActor?.let { canvasActor ->
                canvas.addActor(canvasActor.copy().apply {
                    setPosition(x - x % tileSize, y - y % tileSize)
                })
            }
        }
    }

    data object Eraser : Tool() {

        var isErasing = false

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
            canvas.hit(x, y, false)?.remove()
        }
    }

    data object Fill : Tool() {


    }

    data object Select : Tool() {

    }

    data object Move : Tool() {

    }
}