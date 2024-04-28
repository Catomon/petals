package ctmn.petals.screens

import ctmn.petals.player.Player
import ctmn.petals.player.Team
import ctmn.petals.player.newBluePlayer
import ctmn.petals.player.newRedPlayer
import ctmn.petals.playscreen.*
import ctmn.petals.story.Scenario
import ctmn.petals.unit.*
import ctmn.petals.unit.actors.Alice
import ctmn.petals.unit.actors.SlimeHuge
import ctmn.petals.widgets.addChangeListener
import ctmn.petals.utils.tiled
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
import ctmn.petals.map.loadMap
import ctmn.petals.playstage.getLabels

class TestRoomScreen(private val game: PetalsGame) : Stage(FillViewport(1366f * 0.3f, 768f * 0.3f)), Screen {

    val assets = game.assets

    private val background = VisImage(Texture("back.png"))
    private val bunnyImage = VisImage(Texture("bunny.png"))
    private val label = VisLabel(Const.APP_NAME + " " + Const.APP_VER_NAME)
    private val mapsList = VisList<String>()
    private val loadButton = VisTextButton(">")
    private val backButton = VisTextButton("<")

    inner class TestScenario(levelName: String) : Scenario(name = "Test Scenario", levelFileName = levelName) {

        val redPlayer = Player("RedPlayer", Player.RED, Team.RED)

        init {
            player = Player("BluePlayer", Player.BLUE, Team.BLUE)
            gameEndCondition = EliminateEnemyUnits()

            players.add(player, redPlayer)
        }

        override fun makeScenario(playScreen: PlayScreen) {
            super.makeScenario(playScreen)

            val player = this.player!!

            for(label in playStage.getLabels()) {
                if (label.labelName == "leader_placement") {
                    when (label.data["id"]) {
                        "1" -> playStage.addActor(Alice().apply {
                            leaderID = 1
                            playerId = player.id
                            teamId = player.teamId
                            initView(assets)
                            setPosition(label.x.tiled(), label.y.tiled());
                        })
                        "2" -> playStage.addActor(Alice().apply {
                            leaderID = 2
                            playerId = redPlayer.id;
                            teamId = redPlayer.teamId
                            initView(assets)
                            setPosition(label.x.tiled(), label.y.tiled());
                        })
                    }
                }
            }
        }
    }

    private fun startScenario() {
        val scenario = TestScenario(mapsList.selected)
//        game.screen = StoryPlayScreen(game, scenario, null).also {
//            it.gameType = GameType.CUSTOM
//            it.gameEndCondition = CaptureBases()
//            //it.aiManager.add(EasyDuelBot(scenario.player, it.playStage)) //ai test
//            it.aiManager.add(EasyDuelBot(scenario.redPlayer, it))
//        }
    }

    init {
        Gdx.input.inputProcessor = this

        mapsList.setSize(320f * 0.3f, 320f * 0.3f)
        //mapsList.setScale(0.3f)

        mapsList.addListener(object : ClickListener() {
            override fun clicked(event: InputEvent?, x: Float, y: Float) {
                super.clicked(event, x, y)

                val hit = mapsList.getItemAt(y)

//                if (hit != null)
//                    startScenario()

                if (hit != null) {
                    val bluePlayer = newBluePlayer
                    val redPlayer = newRedPlayer

                    game.screen = PlayScreenTemplate.pvp(
                        game,
                        loadMap(mapsList.selected),
                        Array<Player>().apply { add(bluePlayer, redPlayer) },
                        GameType.PVP_SAME_SCREEN,
                        NoEnd(), //CaptureBases()
                        GameMode.ALL,
                    ).apply {
                        //aiManager.add(EasyDuelBot(redPlayer, this))

                        playStage.addActor(SlimeHuge().player(bluePlayer).position(10, 10))
                    }
                }
            }
        })

        loadButton.addChangeListener {
            startScenario()
        }
        backButton.addChangeListener {
            game.screen = MenuScreen(game)
        }

        addActor(background)
        bunnyImage.setSize(128f * 0.3f, 128f * 0.3f)
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
            viewport.worldWidth / 2 - background.width / 2,
            viewport.worldHeight / 2 - background.height / 2)
        bunnyImage.setPosition(
            viewport.worldWidth / 2 - bunnyImage.width / 2,
            viewport.worldHeight / 2 - bunnyImage.height / 2 + viewport.worldHeight / 4)
        label.setPosition(
            viewport.worldWidth / 2 - label.width / 2,
            viewport.worldHeight / 2 - label.height / 2)
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
            names.add(file.nameWithoutExtension())
        }
        mapsList.setItems(names)
    }

    override fun hide() {

    }
}