package ctmn.petals.playscreen.triggers

import com.badlogic.gdx.scenes.scene2d.EventListener
import ctmn.petals.playscreen.events.UnitAddedEvent
import ctmn.petals.unit.playerId

class UnitAddedTrigger(val playerId: Int = -1) : Trigger() {

    private var eventFired = false

    private val listener = EventListener {
        if (it is UnitAddedEvent) {
            if (playerId < 0 || playerId == it.unit.playerId)
                eventFired = true
        }

        false
    }

    override fun check(delta: Float): Boolean {
        if (eventFired) {

            if (removeOnTrigger)
                playScreen.playStage.removeListener(listener)
            else
                eventFired = false

            return true
        }

        return false
    }

    override fun onAdded() {
        super.onAdded()

        playScreen.playStage.addListener(listener)
    }
}