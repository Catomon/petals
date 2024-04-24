package ctmn.petals.editor

import com.badlogic.gdx.Gdx
import com.badlogic.gdx.InputMultiplexer
import com.badlogic.gdx.ScreenAdapter
import com.badlogic.gdx.graphics.Color
import com.badlogic.gdx.graphics.GL20
import com.badlogic.gdx.graphics.OrthographicCamera
import com.badlogic.gdx.graphics.Texture
import com.badlogic.gdx.graphics.g2d.Sprite
import com.badlogic.gdx.graphics.g2d.SpriteBatch
import com.badlogic.gdx.graphics.glutils.ShapeRenderer
import com.badlogic.gdx.math.Rectangle
import com.badlogic.gdx.utils.viewport.ScreenViewport
import ctmn.petals.Rich
import ctmn.petals.discordRich

class EditorScreen : ScreenAdapter() {

    val batch = SpriteBatch()
    val shapeRenderer = ShapeRenderer()

    val actorsPackage = CanvasActorsPackage()
    val tools = Tools(actorsPackage)

    var canvas = CanvasStage(ScreenViewport(), batch, shapeRenderer)
    var interfaceStage = InterfaceStage(canvas, actorsPackage, tools, batch)

    val prefs = Gdx.app.getPreferences("editor.petals.ctmn")

    private val background = TiledBackgroundDrawer(Sprite(Texture("background_tile.png")))

    init {
        tools.setContext(canvas)

        Gdx.input.inputProcessor = InputMultiplexer(interfaceStage, canvas)

        (canvas.viewport.camera as OrthographicCamera).zoom = 1.75f

        discordRich(Rich.EDITOR)
    }

    override fun show() {

    }

    override fun render(delta: Float) {
        Gdx.gl.glClearColor(0f, 0f, 0f, 1f)
        Gdx.gl.glClear(GL20.GL_COLOR_BUFFER_BIT)

        batch.projectionMatrix = canvas.camera.combined
        batch.begin()
        background.draw(batch, canvas.camera as OrthographicCamera)
        batch.end()

        canvas.act()
        canvas.draw()

        if (tools.current == tools.select) {
            val selectionRect = tools.select.selectionRect

            shapeRenderer.projectionMatrix = canvas.camera.combined
            shapeRenderer.begin()
            shapeRenderer.color = Color.WHITE
            shapeRenderer.rect(selectionRect.x, selectionRect.y, selectionRect.width, selectionRect.height)

            if (!tools.select.selectedActors.isEmpty) {
                shapeRenderer.color = Color.YELLOW
                tools.select.selectedActors.forEach {
                    shapeRenderer.rect(Rectangle.tmp.set(it.x, it.y, it.width, it.height))
                }
            }
            shapeRenderer.end()
        }

        interfaceStage.act()
        interfaceStage.draw()
    }

    override fun resize(width: Int, height: Int) {
        interfaceStage.onScreenResize(width, height)
        canvas.viewport.update(width, height)
    }

    override fun dispose() {
        background.dispose()
        batch.dispose()
        shapeRenderer.dispose()
    }
}