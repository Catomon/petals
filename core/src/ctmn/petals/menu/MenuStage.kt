package ctmn.petals.menu

import ctmn.petals.*
import com.badlogic.gdx.Gdx
import com.badlogic.gdx.scenes.scene2d.Actor
import com.badlogic.gdx.scenes.scene2d.Stage
import com.badlogic.gdx.utils.Align
import com.kotcrab.vis.ui.widget.VisImage
import com.kotcrab.vis.ui.widget.VisLabel
import com.kotcrab.vis.ui.widget.VisTable
import com.kotcrab.vis.ui.widget.VisWindow
import ctmn.petals.editor.EditorScreen
import ctmn.petals.menu.StorySelectStage.Companion.startStory
import ctmn.petals.screens.LevelSelectScreen
import ctmn.petals.screens.MenuScreen
import ctmn.petals.story.QuickplayScreen
import ctmn.petals.story.StoriesManager
import ctmn.petals.utils.addClickListener
import ctmn.petals.widgets.*

class MenuStage(val menuScreen: MenuScreen) : Stage(menuScreen.viewport, menuScreen.batch) {

    private val bunnyImage = VisImage(assets.getTexture("bunny.png"))

    private val label = newLabel(Const.APP_NAME + " " + Const.APP_VER_NAME, "font_5")

    private val storyButton = newTextButton(strings.menu.story).apply { isDisabled = true }
    private val quickPlayButton = newTextButton(strings.menu.quickplay)
    private val matchButton = newTextButton(strings.menu.match)
    private val vsPlayerButton = newTextButton(strings.menu.vsPlayer)
    private val vsBotButton = newTextButton(strings.menu.vsBot)
    private val profileButton = newTextButton(strings.menu.profile)
    private val editorButton = newTextButton(strings.menu.editor)
    private val settingsButton = newTextButton(strings.menu.settings)
    private val exitButton = newTextButton(strings.menu.exit)

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
        matchButton.addChangeListener {
            menuScreen.stage = menuScreen.lobbyTypesStage
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
            menuScreen.stage = menuScreen.lobbyTypesStage
        }

        table.setFillParent(true)
        table.center()

        with(table) {
            add(bunnyImage).size(200f).colspan(2).padBottom(32f)
            row()
            add(storyButton).bottom().width(300f).colspan(2)
            row()
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
        }

        addActor(table)

        table2.setFillParent(true)
        with(table2) {
            bottom()
            add(VisImage("icons/itch").addClickListener {
                Gdx.net.openURI("https://serascout.itch.io/petals")
            }.addClickSound())
            add(VisImage("icons/reddit").addClickListener {
                Gdx.net.openURI("https://www.reddit.com/r/Petals/")
            }.addClickSound())
            add(VisImage("icons/discord").addClickListener {
                Gdx.net.openURI("https://discord.gg/QMG3UeUugF")
            }.addClickSound())
        }

        addActor(table2)

        windowTable.setFillParent(true)
        addActor(windowTable)
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