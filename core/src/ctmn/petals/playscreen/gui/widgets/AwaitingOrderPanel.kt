package ctmn.petals.playscreen.gui.widgets

import com.kotcrab.vis.ui.widget.VisImageButton
import com.kotcrab.vis.ui.widget.VisTable
import ctmn.petals.playscreen.events.CommandExecutedEvent
import ctmn.petals.playscreen.events.NextTurnEvent
import ctmn.petals.playscreen.gui.PlayGUIStage
import ctmn.petals.utils.playerUnitsHasAction
import ctmn.petals.widgets.addChangeListener
import ctmn.petals.widgets.addFocusBorder
import ctmn.petals.widgets.newLabel

class AwaitingOrderPanel(private val guiStage: PlayGUIStage) : VisTable() {

    private val playScreen = guiStage.playScreen
    private val localPlayer = guiStage.localPlayer
    private val playStage = guiStage.playStage

    private val unitsHaveActionButton = VisImageButton("units_have_action").addFocusBorder()
    private val unitsHaveActionCountLabel = newLabel("")

    init {
        unitsHaveActionButton.addChangeListener {
            guiStage.selectNextAvailableUnit()
        }

        guiStage.addListener {
            when (it) {
                is CommandExecutedEvent, is NextTurnEvent -> {
                    val unitsActAw = playStage.playerUnitsHasAction(localPlayer).size
                    unitsHaveActionButton.isDisabled =
                        !(playScreen.turnManager.currentPlayer == localPlayer && unitsActAw > 0)

                    if (unitsActAw > 0 && !unitsHaveActionButton.isDisabled)
                        unitsHaveActionCountLabel.setText(unitsActAw)
                    else
                        unitsHaveActionCountLabel.setText("")
                }
            }

            false
        }

        setFillParent(true)
        guiStage.addActor(this)
        center().right().padRight(12f)

        add(unitsHaveActionCountLabel)
        add(unitsHaveActionButton)
    }
}