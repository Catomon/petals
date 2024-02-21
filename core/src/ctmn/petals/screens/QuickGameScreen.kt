package ctmn.petals.screens

import ctmn.petals.player.Player
import ctmn.petals.player.Team
import ctmn.petals.playscreen.TeamStand
import ctmn.petals.story.Scenario
import ctmn.petals.widgets.addChangeListener
import com.badlogic.gdx.Gdx
import com.badlogic.gdx.Screen
import com.badlogic.gdx.graphics.GL20
import com.badlogic.gdx.graphics.Texture
import com.badlogic.gdx.scenes.scene2d.InputEvent
import com.badlogic.gdx.scenes.scene2d.Stage
import com.badlogic.gdx.scenes.scene2d.utils.ClickListener
import com.badlogic.gdx.utils.Array
import com.badlogic.gdx.utils.viewport.FillViewport
import com.kotcrab.vis.ui.widget.VisImage
import com.kotcrab.vis.ui.widget.VisLabel
import com.kotcrab.vis.ui.widget.VisList
import com.kotcrab.vis.ui.widget.VisTextButton
import ctmn.petals.Const
import ctmn.petals.PetalsGame

class QuickGameScreen(private val game: PetalsGame) : Stage(FillViewport(1366f * 0.3f, 768f * 0.3f)), Screen {

    val assets = game.assets

    private val background = VisImage(Texture("back.png"))
    private val bunnyImage = VisImage(Texture("bunny.png"))
    private val label = VisLabel(Const.APP_NAME + " " + Const.APP_VER_NAME)
    private val mapsList = VisList<String>()
    private val loadButton = VisTextButton("Go")
    private val backButton = VisTextButton("Back")

    inner class TestScenario(levelName: String) : Scenario(name = "Test Scenario", levelFileName = levelName,) {
        val redPlayer = Player("RedPlayer", Player.RED, Team.RED)

        init {
            player = Player("BluePlayer", Player.BLUE, Team.BLUE)
            gameEndCondition = TeamStand(Team.BLUE)

            players.add(player, redPlayer)
        }
    }

    private fun startScenario() {
        val scenario = TestScenario(mapsList.selected)
//        game.screen = StoryPlayScreen(game, scenario).also {
//            it.aiManager.add(EasyAiDuelBot(scenario.redPlayer, it))
//        }
    }

    init {
        Gdx.input.inputProcessor = this

        mapsList.setSize(320f, 320f)

        mapsList.addListener(object : ClickListener() {
            override fun clicked(event: InputEvent?, x: Float, y: Float) {
                super.clicked(event, x, y)

                val hit = mapsList.getItemAt(y)

                if (hit != null)
                    startScenario()
            }
        })

        loadButton.addChangeListener {
            startScenario()
        }
        backButton.addChangeListener {
            game.screen = MenuScreen(game)
        }

        addActor(background)
        bunnyImage.setSize(128f, 128f)
        addActor(bunnyImage)
        addActor(label)
        addActor(mapsList)
        addActor(backButton)
        addActor(loadButton)
    }

    override fun act(delta: Float) {
        super.act(delta)

    }

    override fun render(delta: Float) {
        Gdx.gl.glClearColor(0f, 0f, 0f, 1f)
        Gdx.gl.glClear(GL20.GL_COLOR_BUFFER_BIT)
        act()
        draw()
    }

    override fun show() {
        updateList()
    }

    override fun resize(width: Int, height: Int) {
        viewport.update(width, height)

        background.setPosition(
            viewport.screenWidth / 2 - background.width / 2,
            viewport.screenHeight / 2 - background.height / 2)
        bunnyImage.setPosition(
            viewport.screenWidth / 2 - bunnyImage.width / 2,
            viewport.screenHeight / 2 - bunnyImage.height / 2 + viewport.screenHeight / 4)
        label.setPosition(
            viewport.screenWidth / 2 - label.width / 2,
            viewport.screenHeight / 2 - label.height / 2)
        mapsList.setPosition(
            label.x + label.width / 2 - mapsList.width / 2,
            label.y - mapsList.height)
        backButton.setPosition(mapsList.x - backButton.width - 2, mapsList.y)
        loadButton.setPosition(mapsList.x + mapsList.height - 2, mapsList.y)
    }

    override fun pause() {

    }

    override fun resume() {
        updateList()
    }

    private fun updateList() {
        val files = Gdx.files.internal("maps").list()
        val names = Array<String>()
        for (file in files) {
            names.add(file.name())
        }
        mapsList.setItems(names)
    }

    override fun hide() {

    }
}