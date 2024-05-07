package ctmn.petals.playscreen.gui

import com.badlogic.gdx.scenes.scene2d.Stage
import com.badlogic.gdx.scenes.scene2d.actions.DelayAction
import com.badlogic.gdx.scenes.scene2d.ui.WidgetGroup
import com.kotcrab.vis.ui.VisUI
import com.kotcrab.vis.ui.widget.VisImage
import com.kotcrab.vis.ui.widget.VisTable
import ctmn.petals.actors.actions.OneAction
import ctmn.petals.game
import ctmn.petals.menu.startLevel
import ctmn.petals.playscreen.PlayScreen
import ctmn.petals.screens.MenuScreen
import ctmn.petals.story.StoryPlayScreen
import ctmn.petals.utils.fadeOut
import ctmn.petals.widgets.StageCover
import ctmn.petals.widgets.addChangeListener
import ctmn.petals.widgets.newLabel
import ctmn.petals.widgets.newTextButton

class StoryGameOverMenu(val result: Int, val playScreen: StoryPlayScreen) : WidgetGroup() {

    private val menuTable = VisTable()

    private val story = playScreen.story

    private val message = when (result) {
        0 -> "Fail"
        else -> "Level Cleared!"
    }

    private val stars = VisTable().apply {
        add(VisImage("star_frame").apply { name = "1" })
        add(VisImage("star_frame").apply { name = "2" })
        add(VisImage("star_frame").apply { name = "3" })
        for (i in 1..result) {
            findActor<VisImage>(i.toString()).addAction(DelayAction(0.50f * i).apply {
                action = OneAction {
                    (actor as VisImage).setDrawable(VisUI.getSkin().getDrawable("star"))
                }
            })
        }
    }

    init {
        name = "game_over_menu"
        setFillParent(true)

        with(menuTable) {
            setFillParent(true)
            add(newLabel(message)).padBottom(16f)
            row()
            add(stars)
            row()
            add(newTextButton("Level Select").addChangeListener {
                goToLevelSelect()
            })
            row()
            add(newTextButton("Restart").addChangeListener {
                game.startLevel(playScreen.story, playScreen.story.getScenario(playScreen.currentScenario.id))
            })
            row()
            add(newTextButton("Continue").addChangeListener {
                val nextSc = playScreen.story.createNextUndoneScenario()
                if (nextSc == null)
                    goToLevelSelect()
                else
                    game.startLevel(playScreen.story, nextSc)
            })
        }

        addActor(StageCover(0.5f))
        addActor(menuTable)
    }

    fun goToLevelSelect() {
        stage.addActor(StageCover().fadeInAndThen(OneAction {
            playScreen.game.screen = MenuScreen().apply {
                stage = levelsStage
                stage.fadeOut()
            }
        }))
    }

    override fun setStage(stage: Stage?) {
        val thisStage = this.stage
        if (thisStage is PlayGUIStage)
            thisStage.mapClickDisabled = false

        if (stage is PlayGUIStage)
            stage.mapClickDisabled = true

        super.setStage(stage)
    }
}