package ctmn.petals.playscreen.listeners

import com.badlogic.gdx.scenes.scene2d.Event
import com.badlogic.gdx.scenes.scene2d.EventListener
import ctmn.petals.playscreen.PlayScreen
import ctmn.petals.playscreen.events.ActionCompletedEvent
import ctmn.petals.playscreen.seqactions.AttackAction

class ActionEffectListener(val playScreen: PlayScreen) : EventListener {
    private val playStage = playScreen.playStage

    override fun handle(event: Event): Boolean {
        if (event is ActionCompletedEvent) {
            if (event.action is AttackAction) {
                //playStage.getTile(event.action.targetUnit)?.cutGrass()
            }
        }

        return false
    }
}