package ctmn.petals.menu

import com.badlogic.gdx.Gdx
import com.badlogic.gdx.scenes.scene2d.Actor
import com.badlogic.gdx.scenes.scene2d.Stage
import com.badlogic.gdx.utils.Align
import com.badlogic.gdx.utils.Array
import com.kotcrab.vis.ui.widget.VisImage
import com.kotcrab.vis.ui.widget.VisLabel
import com.kotcrab.vis.ui.widget.VisTable
import com.kotcrab.vis.ui.widget.VisTextButton
import com.kotcrab.vis.ui.widget.VisWindow
import ctmn.petals.Const
import ctmn.petals.assets
import ctmn.petals.bot.EasyDuelBot
import ctmn.petals.editor.EditorScreen
import ctmn.petals.game
import ctmn.petals.map.labels
import ctmn.petals.map.loadMap
import ctmn.petals.menu.StorySelectStage.Companion.startStory
import ctmn.petals.player.Player
import ctmn.petals.player.fairy
import ctmn.petals.player.newBluePlayer
import ctmn.petals.player.newRedPlayer
import ctmn.petals.playscreen.GameMode
import ctmn.petals.playscreen.GameType
import ctmn.petals.playscreen.NoEnd
import ctmn.petals.screens.LevelSelectScreen
import ctmn.petals.screens.MenuScreen
import ctmn.petals.screens.PlayScreenTemplate
import ctmn.petals.screens.quickplay.QuickplayScreen
import ctmn.petals.story.StoriesManager
import ctmn.petals.strings
import ctmn.petals.utils.addClickListener
import ctmn.petals.widgets.addChangeListener
import ctmn.petals.widgets.addClickSound
import ctmn.petals.widgets.newLabel
import ctmn.petals.widgets.newTextButton

class MenuStage(val menuScreen: MenuScreen) : Stage(menuScreen.viewport, menuScreen.batch) {

    private val bunnyImage = VisImage(assets.getTexture("bunny.png"))

    private val label = newLabel(Const.APP_NAME + " " + Const.APP_VER_NAME, "default")

    private val storyButton = newTextButton(strings.general.story).apply { isDisabled = true }
    private val campaignButton = newTextButton("(OLD)" + strings.general.levels)
    private val quickPlayButton = newTextButton(strings.general.quickplay)
    private val matchButton = newTextButton(strings.general.match)
    private val vsPlayerButton = newTextButton(strings.general.vsPlayer)
    private val vsBotButton = newTextButton(strings.general.vsBot)
    private val profileButton = newTextButton(strings.general.profile).apply { isDisabled = true }
    private val editorButton = newTextButton(strings.general.editor)
    private val settingsButton = newTextButton(strings.general.settings)
    private val exitButton = newTextButton(strings.general.exit)

    private val table = VisTable()
    private val table2 = VisTable()
    private val windowTable = VisTable()

    init {
        storyButton.addChangeListener {
            if (StoriesManager.size == 1) {
                val story = StoriesManager.getStories().first()

                if (Const.IS_RELEASE)
                    menuScreen.startStory(story)
                else
                    menuScreen.game.screen = LevelSelectScreen(game, story)
            } else
                menuScreen.stage = menuScreen.storySelectStage
        }
        campaignButton.addChangeListener {
            menuScreen.stage = menuScreen.levelsStage
        }
        matchButton.addChangeListener {
            menuScreen.stage = menuScreen.mpLobbyVariantsStage
        }
        editorButton.addChangeListener {
            game.screen = EditorScreen()
        }
        settingsButton.addChangeListener {
            menuScreen.stage = menuScreen.settingsStage
        }
        exitButton.addChangeListener {
            Gdx.app.exit()
        }
        quickPlayButton.addChangeListener {
            game.screen = QuickplayScreen(game)
        }
        vsBotButton.addChangeListener {
            menuScreen.stage = menuScreen.botGameSetupStage
        }
        vsPlayerButton.addChangeListener {
            menuScreen.stage = menuScreen.mpLobbyVariantsStage
        }

        table.setFillParent(true)
        table.center()

        with(table) {
            add(bunnyImage).size(bunnyImage.width * 200f / bunnyImage.height, 200f).padBottom(32f)
            row()
            add(VisTable().apply {
//            add(storyButton).bottom().width(300f).colspan(2)
//            row()
                add(campaignButton).bottom().width(300f).colspan(2)
                row()

                if (Const.DEBUG_MODE) {
                    add(VisTextButton("RNG GEN TEST").addClickListener {
                        game.screen = QuickplayScreen(game)
                    })
                    add(newTextButton("TEST PLAY").addChangeListener {
                        startTestPlay()
                    })
                    row()
                }

                add(vsPlayerButton).width(150f)
                add(vsBotButton).width(150f)
                row()
                add(profileButton).width(150f)
                add(editorButton).width(150f)
                row()
                add(settingsButton).bottom().width(150f)
                add(exitButton).bottom().width(150f)
                row()
                add(VisLabel("ver " + Const.APP_VER_NAME).apply {
                    setFontScale(0.65f)
                    align(Align.center)
                }).colspan(2).bottom().padTop(30f)
                row().height(30f)
            }).width(320f)
        }

        addActor(table)

        table2.setFillParent(true)
        with(table2) {
            bottom()
            add(VisImage("icons/itch").addClickListener {
                Gdx.net.openURI("https://monscout.itch.io/faesfeistyfray")
            }.addClickSound())
        }

        addActor(table2)

        windowTable.setFillParent(true)
        addActor(windowTable)
    }

    private fun startTestPlay() {
        val players = Array<Player>().apply {
            addAll(
                newBluePlayer.apply { credits = 99999 },
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

    override fun addActor(actor: Actor?) {
        if (actor is VisWindow) {
            actor.isMovable = false
            windowTable.clear()
            windowTable.add(actor).expand().center().maxWidth(viewport.worldWidth - 10f)
            return
        }

        super.addActor(actor)
    }
}