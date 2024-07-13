package ctmn.petals.playscreen.gui.widgets

import com.badlogic.gdx.scenes.scene2d.actions.Actions
import com.kotcrab.vis.ui.widget.VisTable
import ctmn.petals.playscreen.events.NextTurnEvent
import ctmn.petals.playscreen.gui.PlayGUIStage
import ctmn.petals.utils.addListener
import ctmn.petals.widgets.newLabel

class NextTurnPopUp(private val guiStage: PlayGUIStage) : VisTable() {

    private val label = newLabel()

    init {
        setFillParent(true)
        label.setFontScale(2f)
        label.pack()
        label.color.a = 0f

        guiStage.addListener<NextTurnEvent> {
            label.actions.clear()
            if (nextPlayer == guiStage.localPlayer) {
                label.setText("TURN START")
                label.addAction(Actions.sequence(Actions.fadeIn(0.2f), Actions.delay(1f), Actions.fadeOut(0.2f)))
            } else {
                if (previousPlayer == guiStage.localPlayer) {
                    label.setText("TURN END")
                    label.addAction(Actions.sequence(Actions.fadeIn(0.2f), Actions.delay(1f), Actions.fadeOut(0.2f)))
                }
            }
        }

        add(label).center()
    }
}