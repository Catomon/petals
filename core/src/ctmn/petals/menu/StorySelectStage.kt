package ctmn.petals.menu

import ctmn.petals.screens.MenuScreen
import ctmn.petals.story.SavesManager
import ctmn.petals.story.StoriesManager
import ctmn.petals.story.Story
import ctmn.petals.story.StoryPlayScreen
import ctmn.petals.story.alissa.AlissaStorySave
import ctmn.petals.widgets.addChangeListener
import ctmn.petals.utils.fadeInAndThen
import ctmn.petals.widgets.ButtonsScrollPane
import ctmn.petals.widgets.newTextButton
import com.badlogic.gdx.scenes.scene2d.Stage
import com.badlogic.gdx.utils.Array
import com.kotcrab.vis.ui.VisUI
import com.kotcrab.vis.ui.widget.*
import ctmn.petals.Const.IS_RELEASE
import ctmn.petals.Const.TREASURED_PETALS_STORY_ID

class StorySelectStage(private val menuScreen: MenuScreen) : Stage(menuScreen.viewport, menuScreen.batch) {

    val assets = menuScreen.game.assets

    private val stories = Array<Story>()

    private val storyCoverImage = VisImage("images/story_cover")
    private val storiesList = ButtonsScrollPane()
    private val returnButton = newTextButton("Return")
    private val confirmButton = newTextButton("Confirm")

    private val table = VisTable()

    private var selectedStory: Story? = null

    init {
        storiesList.setForceScroll(false, false)
        storiesList.setSize(100f, 100f)

        //add listeners
        storiesList.onButtonClick = {button ->
            selectedStory = stories.first { story -> story.name.contentEquals(button.text) }

            val coverName = when (selectedStory?.id) {
                TREASURED_PETALS_STORY_ID -> "alissa_story_cover"

                else -> "story_cover"
            }

            storyCoverImage.setDrawable(VisUI.getSkin(), "images/$coverName")
        }

        returnButton.addChangeListener {
            menuScreen.stage = menuScreen.menuStage
        }

        confirmButton.addChangeListener {
            //prepare and start screen
            if (selectedStory != null) {
                menuScreen.startStory(selectedStory!!)
            }
        }

        //setup table
        table.setFillParent(true)
        //table.debug = true

        with (table) {
            add(storyCoverImage).pad(2f).size(100f)
            row()
            add(storiesList).expandY()
            row()
            add(VisTable().apply {
                //debug = true
                add(returnButton).left()
                add().expandX()
                add(confirmButton).right()
            }).width(100f)
        }

        addActor(table)

        //reset state event listener
        addListener {
            if (it is ResetStateEvent) {
                selectedStory = null

                storyCoverImage.setDrawable(VisUI.getSkin(), "images/story_cover")
                storiesList.scrollPercentY = 0f
                storiesList.velocityY = 0f
                storiesList.updateVisualScroll()

                storiesList.uncheckButtons()
            }

            false
        }

        //load stories
        stories.addAll(StoriesManager.getStories())

        storiesList.setItems(Array<String>().apply { stories.forEach { this.add(it.name) } })
    }

    companion object {
        fun MenuScreen.startStory(story: Story) {
            val menuScreen = this

            with(stage) {
                when (story.id) {
                    TREASURED_PETALS_STORY_ID -> story.storySave =
                        SavesManager.getByName<AlissaStorySave>(story.storySave.save_name) ?: AlissaStorySave().apply { save_name = "treasured_petals"; story_id = TREASURED_PETALS_STORY_ID }
                    else -> {
                        //throw IllegalStateException("Unknown story id: $story.id")
                    }
                }

                val addTime = if (story.storySave.progress == 0) 2f else 0f
                if (IS_RELEASE)
                    fadeInAndThen(2f, addTime) {

                        menuScreen.game.screen = StoryPlayScreen(menuScreen.game, story).apply {
                            friendlyFire = false
                        }
                    }
                else
                    menuScreen.game.screen = StoryPlayScreen(menuScreen.game, story).apply {
                        friendlyFire = false
                    }
            }
        }
    }
}