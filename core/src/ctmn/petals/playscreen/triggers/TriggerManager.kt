package ctmn.petals.playscreen.triggers

import ctmn.petals.playscreen.PlayScreen
import com.badlogic.gdx.utils.Array

class TriggerManager(val playScreen: PlayScreen) {

    private val triggers = Array<Trigger>()

    fun update(delta: Float) {
        for (trigger in triggers) {
            if (trigger.check(delta)) {
                trigger.onTrigger?.invoke(playScreen)

                if (trigger.removeOnTrigger)
                    triggers.removeValue(trigger, false)
            }
        }
    }

    fun addTrigger(trigger: Trigger) {
        trigger.playScreen = playScreen

        triggers.add(trigger)

        trigger.onAdded()
    }
}