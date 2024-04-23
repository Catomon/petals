package ctmn.petals.screens

import ctmn.petals.utils.*
import ctmn.petals.widgets.MovingBackground
import com.badlogic.gdx.Application
import com.badlogic.gdx.Gdx
import com.badlogic.gdx.Screen
import com.badlogic.gdx.graphics.Color
import com.badlogic.gdx.graphics.GL20
import com.badlogic.gdx.graphics.g2d.Sprite
import com.badlogic.gdx.graphics.g2d.SpriteBatch
import com.badlogic.gdx.scenes.scene2d.Stage
import com.badlogic.gdx.utils.Array
import com.badlogic.gdx.utils.viewport.ExtendViewport
import ctmn.petals.*
import ctmn.petals.menu.*
import ctmn.petals.widgets.ParallaxBackground

class MenuScreen(val game: PetalsGame = ctmn.petals.game) : Screen {

    val viewport = ExtendViewport(32f, 720f)

    val batch = SpriteBatch()

    private val music = AudioManager.music("optomistic_day_masteredloopable.ogg").apply { isLooping = true }

    private val background = ParallaxBackground(Array<MovingBackground>().apply {
        add(MovingBackground(assets.getTexture("1.png"), 1f))
        add(MovingBackground(assets.getTexture("2.png"), 2f))
        add(MovingBackground(assets.getTexture("3.png"), 3f))
        add(MovingBackground(assets.getTexture("4.png"), 4f))
    })

    val storySelectStage = StorySelectStage(this)
    val menuStage = MenuStage(this)
    val lobbyTypesStage = LobbyTypesStage(this)
    val botGameSetupStage = CustomGameSetupStage(this)
    val mapSelectionStage = MapSelectionStage(this) {}
    val settingsStage = SettingsStage(this)

    private val blackThingy =
        Sprite(assets.findAtlasRegion("gui/white")).also { it.color = Color.BLACK; it.setAlpha(0.5f) }

    var stage: Stage = menuStage
        set(value) {
            field = value
            field.root.fire(ResetStateEvent())
            field.root.fire(ScreenSizeChangedEvent(Gdx.graphics.width, Gdx.graphics.height))

            Gdx.input.inputProcessor = field
        }

    init {
        discordRich(Rich.DEFAULT)

        Gdx.input.inputProcessor = stage

        batch.projectionMatrix = viewport.camera.combined
    }

    override fun render(delta: Float) {
        Gdx.gl.glClearColor(0f, 0f, 0f, 1f)
        Gdx.gl.glClear(GL20.GL_COLOR_BUFFER_BIT)

        background.act(delta)

        batch.begin()
        background.draw(stage.batch, stage.root.color.a)

        blackThingy.x = viewport.camera.position.x - 150f
        blackThingy.setSize(300f, stage.height)
        blackThingy.draw(stage.batch, stage.root.color.a)
        batch.end()

        stage.act()
        stage.draw()
    }

    override fun hide() {
        music.pause()
        music.position = 0f
    }

    override fun show() {
        music.play()
    }

    override fun pause() {

    }

    override fun resume() {

    }

    override fun resize(width: Int, height: Int) {
        if (Gdx.app.type == Application.ApplicationType.Android)
            if (Const.IS_PORTRAIT) {
                viewport.minWorldWidth = width / Const.GUI_SCALE / 1.25F
                viewport.minWorldHeight = height / Const.GUI_SCALE / 1.25F
            } else {
                viewport.minWorldWidth = width / Const.GUI_SCALE
                viewport.minWorldHeight = height / Const.GUI_SCALE
            }

        viewport.update(width, height)

        viewport.camera.resetPosition()

        stage.root.fire(ScreenSizeChangedEvent(width, height))

        background.height = viewport.worldHeight
        background.setPosByCenter(stage.worldCenterX, stage.worldCenterY)
    }

    override fun dispose() {
        menuStage.dispose()
        storySelectStage.dispose()
        batch.dispose()
        AudioManager.disposeMusic()
    }
}
