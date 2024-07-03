package ctmn.petals.playscreen.triggers

import com.badlogic.gdx.utils.Array
import ctmn.petals.playscreen.events.CommandExecutedEvent
import ctmn.petals.unit.UnitActor
import ctmn.petals.unit.tiledX
import ctmn.petals.unit.tiledY
import ctmn.petals.utils.addOneTimeListener

class UnitVisibleTrigger(val units: Array<UnitActor>) : Trigger() {

    private var isTriggered = false

    override fun check(delta: Float): Boolean {
        return isTriggered
    }

    override fun onAdded() {
        super.onAdded()

        playScreen.playStage.addOneTimeListener<CommandExecutedEvent> {
            if (units.any { playScreen.fogOfWarManager.isVisible(it.tiledX, it.tiledY) } || units.isEmpty()) {
                isTriggered = true
            }

            false
        }
    }
}