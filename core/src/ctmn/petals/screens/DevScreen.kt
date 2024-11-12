package ctmn.petals.screens

import com.badlogic.gdx.Application
import com.badlogic.gdx.Gdx
import com.badlogic.gdx.Screen
import com.badlogic.gdx.graphics.Color
import com.badlogic.gdx.graphics.GL20
import com.badlogic.gdx.graphics.g2d.Sprite
import com.badlogic.gdx.math.Vector2
import com.badlogic.gdx.scenes.scene2d.Stage
import com.badlogic.gdx.scenes.scene2d.ui.VerticalGroup
import com.badlogic.gdx.utils.Array
import com.badlogic.gdx.utils.viewport.ExtendViewport
import com.kotcrab.vis.ui.widget.VisTable
import com.kotcrab.vis.ui.widget.VisTextButton
import ctmn.petals.AudioManager
import ctmn.petals.Const
import ctmn.petals.PetalsGame
import ctmn.petals.bot.EasyDuelBot
import ctmn.petals.assets
import ctmn.petals.map.labels
import ctmn.petals.map.loadMap
import ctmn.petals.player.*
import ctmn.petals.playscreen.GameMode
import ctmn.petals.playscreen.GameType
import ctmn.petals.playscreen.NoEnd
import ctmn.petals.screens.quickplay.QuickplayScreen
import ctmn.petals.utils.*
import ctmn.petals.widgets.MovingBackground
import ctmn.petals.widgets.addChangeListener
import ctmn.petals.widgets.newTextButton
import kotlin.random.Random

@Deprecated("stuff was moved to the main menu")
class DevScreen(val game: PetalsGame) : Stage(ExtendViewport(32f, 720f)), Screen {

    private val background = MovingBackground(assets.getTexture("sky.png"), 5f)

    private val blackThingy =
        Sprite(assets.findAtlasRegion("gui/white")).also { it.color = Color.BLACK; it.setAlpha(0.5f) }

    private val testMapName = "test"

    init {
        Gdx.input.inputProcessor = this
        batch.projectionMatrix = viewport.camera.combined

        addWidgets()
        AudioManager.sound("click")
    }

    private fun addWidgets() { //todo move to settings and main screen
        addActor(VisTable().apply {
            setFillParent(true)

            add(VerticalGroup().also { group ->
                group.addActor(newTextButton("Test Play").addChangeListener {
                    startTestPlay()
                })
                group.addActor(newTextButton("Random Map").addChangeListener {
                    ctmn.petals.game.screen = QuickplayScreen(ctmn.petals.game)
                })
            })
            add(VerticalGroup().also { group ->
                group.addActor(newTextButton("Menu Screen").addChangeListener {
                    game.screen = MenuScreen(game)
                })
                group.addActor(VisTextButton("Reload Assets").addChangeListener {
                    game.runTexturePacker.run()
                    game.assets.clear()
                    game.screen = LoadingScreen(game)
                })
            })
        })
    }

    private fun startTestPlay() {
        val players = Array<Player>().apply {
            addAll(newBluePlayer.apply { credits = 99999 },
                newRedPlayer.apply { species = fairy })
        }
        val ps = PlayScreenTemplate.pvp(
            game,
            loadMap("test"),
            players,
            GameType.PVP_SAME_SCREEN,
            NoEnd(),
            GameMode.CRYSTALS_LEADERS,
            players.first(),
        ).apply {
            botManager.add(EasyDuelBot(players[1], this))
            fogOfWarManager.drawFog = false
        }

        ps.map?.labels?.forEach { label ->
            if (label.labelName == "player") {
                label.data.put("player_id", (label.data["id"].toInt() + 1).toString())
            }
        }

        ps.ready()

        game.screen = ps
    }

    private val unitsAtlasRandom = assets.findUnitAtlas(Random.nextInt(1, 9)).textures.firstOrNull()

    override fun render(delta: Float) {
        Gdx.gl.glClearColor(0f, 0f, 0f, 1f)
        Gdx.gl.glClear(GL20.GL_COLOR_BUFFER_BIT)

        background.act(delta)

        batch.begin()
        background.draw(batch, root.color.a)

        blackThingy.x = viewport.camera.position.x - 150f
        blackThingy.setSize(300f, height)
        blackThingy.draw(batch, root.color.a)

        if (unitsAtlasRandom != null) {
            val scr = screenToStageCoordinates(Vector2(Gdx.input.x.toFloat(), Gdx.input.y.toFloat()))
            batch.draw(unitsAtlasRandom, scr.x - unitsAtlasRandom.width / 2f, scr.y - unitsAtlasRandom.height / 2f)
        }

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
