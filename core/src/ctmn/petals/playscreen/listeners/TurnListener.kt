package ctmn.petals.playscreen.listeners

import ctmn.petals.playstage.PlayStage
import ctmn.petals.playscreen.events.PlayStageListener
import com.badlogic.gdx.scenes.scene2d.Event

/** does something at player's turn */
open class TurnListener(val playerID: Int, val onPlayerTurn: (PlayStage, TurnListener) -> Unit) : PlayStageListener() {

    override fun handle(event: Event?): Boolean {
        if (event is TurnsCycleListener.TurnCycleEvent) {
            if (event.nextPlayer.id == playerID) {
                onPlayerTurn(this.playStage, this)
            }
        }

        return false
    }
}