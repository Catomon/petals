package ctmn.petals.editor

import com.badlogic.gdx.Gdx
import com.badlogic.gdx.InputMultiplexer
import com.badlogic.gdx.ScreenAdapter
import com.badlogic.gdx.graphics.GL20
import com.badlogic.gdx.graphics.OrthographicCamera
import com.badlogic.gdx.graphics.Texture
import com.badlogic.gdx.graphics.g2d.Sprite
import com.badlogic.gdx.graphics.g2d.SpriteBatch
import com.badlogic.gdx.scenes.scene2d.Stage
import com.badlogic.gdx.utils.viewport.ScreenViewport
import ctmn.petals.utils.TiledBackground

class EditorScreen : ScreenAdapter() {

    val actorsPackage = CanvasActorsPackage()

    val batch = SpriteBatch()

    var canvas = Stage(ScreenViewport(), batch)
    var gui = InterfaceStage(this, actorsPackage, batch)

    val prefs = Gdx.app.getPreferences("editor.petals.ctmn")

    private val backImage = CanvasBackground(Sprite(Texture("background_tile.png")))

    init {
        Tool.setCanvas(canvas)

        Gdx.input.inputProcessor = InputMultiplexer(gui, canvas)

        (canvas.viewport.camera as OrthographicCamera).zoom = 1.75f
    }

    override fun show() {

    }

    override fun render(delta: Float) {
        Gdx.gl.glClearColor(0f, 0f, 0f, 1f)
        Gdx.gl.glClear(GL20.GL_COLOR_BUFFER_BIT)

        batch.projectionMatrix = canvas.camera.combined
        batch.begin()
        backImage.draw(batch, canvas.camera as OrthographicCamera)
        batch.end()

        canvas.act()
        canvas.draw()

        gui.act()
        gui.draw()
    }

    override fun resize(width: Int, height: Int) {
        gui.onScreenResize(width, height)
        canvas.viewport.update(width, height)
    }

    override fun dispose() {

    }
}