package ctmn.petals.menu

import com.badlogic.gdx.scenes.scene2d.Actor
import com.badlogic.gdx.scenes.scene2d.Stage
import com.kotcrab.vis.ui.VisUI
import com.kotcrab.vis.ui.widget.VisImage
import com.kotcrab.vis.ui.widget.VisImageButton
import com.kotcrab.vis.ui.widget.VisLabel
import com.kotcrab.vis.ui.widget.VisTable
import com.kotcrab.vis.ui.widget.VisWindow
import ctmn.petals.Const
import ctmn.petals.Const.IS_RELEASE
import ctmn.petals.PetalsGame
import ctmn.petals.screens.MenuScreen
import ctmn.petals.story.*
import ctmn.petals.story.faecampaign.FaerieStory
import ctmn.petals.story.faecampaign.FaerieStorySave
import ctmn.petals.utils.log
import ctmn.petals.widgets.*

fun PetalsGame.startLevel(story: Story, scenario: Scenario) {
    val game = this
    val playScreen = StoryPlayScreen(game, story, scenario)
    playScreen.setDefaultPlayerIdToLabels()
    playScreen.ready()
    game.screen = playScreen
}

class LevelsStage(private val menuScreen: MenuScreen) : Stage(menuScreen.viewport, menuScreen.batch) {

    private val game = menuScreen.game

    private val labelChooseLevel = newLabel("Select level", "font_5")

    private val returnButton = newTextButton("Return")

    private val table = VisTable()
    private val windowTable = VisTable()

    private val story =
        FaerieStory(SavesManager.getByStoryId<FaerieStorySave>(Const.FAERIE_CAMPAIGN_ID) ?: FaerieStorySave())

    inner class LevelButton(levelNum: Int, drawableName: String = "forest_level") : VisImageButton("level") {

        private val label = VisLabel(levelNum.toString())
        private val statusImage = VisImage()

        init {
            style.imageUp = VisUI.getSkin().newDrawable(drawableName)
            addClickSound()
            addFocusBorder()

            label.pack()
            label.setPosition(width / 2 - label.width / 2, height / 2 - label.height / 2)
            addActor(label)

            statusImage.setSize(width, height)
            val levelProgress = story.storySave.progress.levels[story.idOf(levelNum - 1)]
            val nextLvlIndex = story.nextScenarioIndex()

            when {
                levelProgress != null && levelProgress.state > 0 -> {
                    statusImage.setDrawable(VisUI.getSkin().newDrawable("completed_level"))

                    addActorBefore(
                        label, VisImage(
                            when (levelProgress.state) {
                                1 -> "1_star"
                                2 -> "2_star"
                                else -> "3_star"
                            }
                        ).also { it.y = 16f }
                    )
                }

                levelNum - 1 <= nextLvlIndex -> {
                    statusImage.setDrawable(VisUI.getSkin().newDrawable("unlocked_level"))
                }

                else -> if (!IS_RELEASE) isDisabled = true
            }
            addActorBefore(children.first(), statusImage)
        }
    }

    init {
        log(story.storySave.toString())

//        addListener {
//            if (it is ResetStateEvent) {
//                ipTextField.clearText()
//            }
//
//            false
//        }

        story.addScenarios()

        addActor(table)
        with(table) {
            setFillParent(true)
            center()

            add(labelChooseLevel).padTop(32f)
            row()

            add(VisTable().apply {
                var curCol = 0
                for (i in 0 until story.size) {
                    val levelType = when {
                        i <= 8 -> "forest_level"
                        i <= 16 -> "road_level"
                        else -> "mountain_level"
                    }
                    add(LevelButton(i + 1, levelType).addChangeListener {
                        game.startLevel(story, story.getScenario(i))
                    })
                    curCol++
                    if (curCol == 4) {
                        row()
                        curCol = 0
                    }
                }
            }).expandY().width(300f)
            row()

            add(returnButton).padTop(24f).bottom().left()

            //padBottom(30f)
        }

        windowTable.setFillParent(true)
        addActor(windowTable)

        returnButton.addChangeListener {
            menuScreen.stage = menuScreen.menuStage
        }
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