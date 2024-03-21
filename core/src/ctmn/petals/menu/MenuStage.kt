package ctmn.petals.menu

import ctmn.petals.*
import com.badlogic.gdx.Gdx
import com.badlogic.gdx.scenes.scene2d.Actor
import com.badlogic.gdx.scenes.scene2d.Stage
import com.kotcrab.vis.ui.widget.VisImage
import com.kotcrab.vis.ui.widget.VisTable
import com.kotcrab.vis.ui.widget.VisWindow
import ctmn.petals.editor.EditorScreen
import ctmn.petals.menu.StorySelectStage.Companion.startStory
import ctmn.petals.screens.LevelSelectScreen
import ctmn.petals.screens.MenuScreen
import ctmn.petals.story.QuickplayScreen
import ctmn.petals.story.StoriesManager
import ctmn.petals.widgets.*

class MenuStage(val menuScreen: MenuScreen) : Stage(menuScreen.viewport, menuScreen.batch) {

    private val bunnyImage = VisImage(assets.getTexture("bunny.png"))

    private val label = newLabel(Const.APP_NAME + " " + Const.APP_VER_NAME, "font_5")

    private val storyButton = newTextButton(strings.menu.story).apply { isDisabled = true }
    private val quickPlayButton = newTextButton(strings.menu.quickplay)
    private val matchButton = newTextButton(strings.menu.match)
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

        bunnyImage.setSize(300f, 300f)

        table.setFillParent(true)
        table.center()

        with (table) {
            add(bunnyImage).width(300f)
            row()
            add(label).bottom()
            row()
            add(storyButton).bottom()
            row()
            add(quickPlayButton)
            row()
            add(matchButton).bottom()
            row()
            add(editorButton)
            row()
            add(settingsButton).bottom()
            row()
            add(exitButton).bottom()

            //padBottom(30f)
        }

        addActor(table)

        table2.setFillParent(true)
        with(table2) {
            bottom()

            // social buttons i guess
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