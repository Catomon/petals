package ctmn.petals.playscreen.gui

import com.badlogic.gdx.scenes.scene2d.Stage
import com.badlogic.gdx.scenes.scene2d.ui.WidgetGroup
import com.kotcrab.vis.ui.widget.VisTable
import ctmn.petals.game
import ctmn.petals.menu.startLevel
import ctmn.petals.playscreen.PlayScreen
import ctmn.petals.story.StoryPlayScreen
import ctmn.petals.widgets.StageCover
import ctmn.petals.widgets.addChangeListener
import ctmn.petals.widgets.newLabel
import ctmn.petals.widgets.newTextButton

class InGameMenu(val playScreen: PlayScreen) : WidgetGroup() {

    val menuTable = VisTable()

    init {
        name = "game_menu"

        setFillParent(true)

        with(menuTable) {
            setFillParent(true)
            add(newLabel("Game Menu")).padBottom(16f)
            row()
            add(newTextButton("Continue").addChangeListener {
                this@InGameMenu.remove()
            })
            row()
            if (playScreen is StoryPlayScreen) {
                add(newTextButton("Restart").addChangeListener {
                    game.startLevel(playScreen.story, playScreen.story.getScenario(playScreen.currentScenario.id))
                })
                row()
            }
            add(newTextButton("Settings").apply { isDisabled = true })
            row()
            add(newTextButton("End game").addChangeListener {
                playScreen.returnToMenuScreen()
            })
        }

        addActor(StageCover(0.5f))
        addActor(menuTable)
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