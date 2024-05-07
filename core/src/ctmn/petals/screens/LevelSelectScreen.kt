package ctmn.petals.screens

import com.badlogic.gdx.Application
import com.badlogic.gdx.Gdx
import com.badlogic.gdx.Screen
import com.badlogic.gdx.graphics.Color
import com.badlogic.gdx.graphics.GL20
import com.badlogic.gdx.graphics.g2d.Sprite
import com.badlogic.gdx.scenes.scene2d.Stage
import com.badlogic.gdx.utils.viewport.ExtendViewport
import com.kotcrab.vis.ui.widget.VisScrollPane
import com.kotcrab.vis.ui.widget.VisTable
import ctmn.petals.Const
import ctmn.petals.PetalsGame
import ctmn.petals.assets
import ctmn.petals.story.Story
import ctmn.petals.story.StoryPlayScreen
import ctmn.petals.utils.*
import ctmn.petals.widgets.MovingBackground
import ctmn.petals.widgets.addChangeListener
import ctmn.petals.widgets.newTextButton

class LevelSelectScreen(val game: PetalsGame, story: Story) : Stage(ExtendViewport(32f, 240f)), Screen {

    private val background = MovingBackground(assets.getTexture("sky.png"), 5f)

    private val blackThingy = Sprite(assets.findAtlasRegion("gui/white")).also { it.color = Color.BLACK; it.setAlpha(0.5f) }

    private val levelsButonsTable = VisTable()
    private val scrollPane = VisScrollPane(levelsButonsTable)

    init {
        Gdx.input.inputProcessor = this
        batch.projectionMatrix = viewport.camera.combined

        if (!story.scenariosAdded)
            story.addScenarios()

        for (i in 0 until story.size) {
            val id = story.idOf(i)
            levelsButonsTable.add(newTextButton("$i. $id").apply {
                userObject = i
                addChangeListener {
                    game.screen = StoryPlayScreen(game, story, story.getScenario(i))
                }
            })
            levelsButonsTable.row()
        }

        addActor(VisTable().apply {
            setFillParent(true)
            add(scrollPane)
        })
    }

    override fun render(delta: Float) {
        Gdx.gl.glClearColor(0f, 0f, 0f, 1f)
        Gdx.gl.glClear(GL20.GL_COLOR_BUFFER_BIT)

        background.act(delta)

        batch.begin()
        background.draw(batch, root.color.a)

        blackThingy.x = viewport.camera.position.x - 150f
        blackThingy.setSize(100f, height)
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
    }

    override fun dispose() {
        batch.dispose()
    }
}
