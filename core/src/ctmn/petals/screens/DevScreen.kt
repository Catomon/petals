package ctmn.petals.screens

import com.badlogic.gdx.Application
import com.badlogic.gdx.Gdx
import com.badlogic.gdx.Screen
import com.badlogic.gdx.graphics.Color
import com.badlogic.gdx.graphics.GL20
import com.badlogic.gdx.graphics.g2d.Sprite
import com.badlogic.gdx.scenes.scene2d.Stage
import com.badlogic.gdx.scenes.scene2d.ui.VerticalGroup
import com.badlogic.gdx.utils.viewport.ExtendViewport
import com.kotcrab.vis.ui.widget.VisTable
import com.kotcrab.vis.ui.widget.VisTextButton
import ctmn.petals.Const
import ctmn.petals.PetalsGame
import ctmn.petals.assets
import ctmn.petals.editor.EditorScreen
import ctmn.petals.story.QuickplayScreen
import ctmn.petals.utils.*
import ctmn.petals.widgets.MovingBackground
import ctmn.petals.widgets.addChangeListener
import ctmn.petals.widgets.newTextButton
import kotlin.random.Random

class DevScreen(val game: PetalsGame) : Stage(ExtendViewport(32f, 720f)), Screen {

    private val background = MovingBackground(assets.getTexture("sky.png"), 5f)

    private val blackThingy = Sprite(assets.findAtlasRegion("gui/white")).also { it.color = Color.BLACK; it.setAlpha(0.5f) }

    private val testMapName = "test"

    init {
        Gdx.input.inputProcessor = this
        batch.projectionMatrix = viewport.camera.combined

        addWidgets()
    }

    private fun addWidgets() {
        addActor(VisTable().apply {
            setFillParent(true)

            add(VerticalGroup().also {  group ->
                group.addActor(newTextButton("Menu Screen").addChangeListener {
                    game.screen = MenuScreen(game)
                })
                group.addActor(VisTextButton("Reload Assets").addChangeListener {
                    game.assets.clear()
                    game.screen = LoadingScreen(game)
                })
            })
        })
    }

    override fun render(delta: Float) {
        Gdx.gl.glClearColor(0f, 0f, 0f, 1f)
        Gdx.gl.glClear(GL20.GL_COLOR_BUFFER_BIT)

        background.act(delta)

        batch.begin()
        background.draw(batch, root.color.a)

        blackThingy.x = viewport.camera.position.x - 150f
        blackThingy.setSize(300f, height)
        blackThingy.draw(batch, root.color.a)
        batch.end()

        act()
        draw()
    }

    override fun hide() {

    }

    override fun show() {

    }

    override fun pause() {

    }

    override fun resume() {

    }

    override fun resize(width: Int, height: Int) {
        val viewport = this.viewport as ExtendViewport

        if (Gdx.app.type == Application.ApplicationType.Android)
            if (Const.IS_PORTRAIT)
                viewport.minWorldHeight = 240f
            else viewport.minWorldHeight = 180f // 180f is too small for custom match stage

        (viewport as ExtendViewport).minWorldWidth = width / Const.GUI_SCALE
        (viewport as ExtendViewport).minWorldHeight = height / Const.GUI_SCALE

        viewport.update(width, height)

        viewport.camera.resetPosition()

        root.fire(ScreenSizeChangedEvent(width, height))

        background.setPosByCenter(worldCenterX, worldCenterY)
        background.x = Random.nextInt((-background.sprite.width).toInt(), background.sprite.width.toInt()).toFloat()
    }

    override fun dispose() {
        Gdx.input.inputProcessor = null
        batch.dispose()
    }
}
