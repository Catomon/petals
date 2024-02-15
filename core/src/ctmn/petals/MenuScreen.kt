package ctmn.petals

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
import ctmn.petals.menu.*
import ctmn.petals.widgets.ParallaxBackground

class MenuScreen(val game: PetalsGame) : Screen {

    val viewport = ExtendViewport(32f, 720f)

    val batch = SpriteBatch()

    private val background = ParallaxBackground(Array<MovingBackground>().apply {
        add(MovingBackground(assets.getTexture("1.png"), 1f))
        add(MovingBackground(assets.getTexture("2.png"), 2f))
        add(MovingBackground(assets.getTexture("3.png"), 3f))
        add(MovingBackground(assets.getTexture("4.png"), 4f))
    })

    val storySelectStage = StorySelectStage(this)
    val menuStage = MenuStage(this)
    val lobbyTypesStage = LobbyTypesStage(this)
    val customGameSetupStage = CustomGameSetupStage(this)
    val mapSelectionStage = MapSelectionStage(this) {}
    val languageSelectStage = LanguageSelectStage(this)
    val settingsStage = SettingsStage(this)

    private val blackThingy = Sprite(assets.findAtlasRegion("gui/white")).also { it.color = Color.BLACK; it.setAlpha(0.5f) }

    var stage: Stage = menuStage
        set(value) {
            field = value
            field.root.fire(ResetStateEvent())

            Gdx.input.inputProcessor = field
        }

    init {
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
        
    }

    override fun show() {

    }

    override fun pause() {
        
    }

    override fun resume() {
        
    }

    override fun resize(width: Int, height: Int) {
        if (Gdx.app.type == Application.ApplicationType.Android)
            if (GameConst.IS_PORTRAIT)
                viewport.minWorldHeight = 240f
            else viewport.minWorldHeight = 180f // 180f is too small for custom match stage

        //code above is overwritten by code below btw

        (viewport as ExtendViewport).minWorldWidth = width / GameConst.GUI_SCALE
        (viewport as ExtendViewport).minWorldHeight = height / GameConst.GUI_SCALE

        viewport.update(width, height)

        viewport.camera.resetPosition()

        stage.root.fire(ScreenSizeChangedEvent(width, height))

        background.setPosByCenter(stage.worldCenterX, stage.worldCenterY)
    }

    override fun dispose() {

        menuStage.dispose()
        storySelectStage.dispose()
        batch.dispose()
    }
}
