package ctmn.petals.playscreen.triggers

import ctmn.petals.player.Player
import ctmn.petals.playscreen.events.NextTurnEvent
import ctmn.petals.utils.addOneTimeListener

class TurnStartTrigger(val player: Player) : Trigger() {

    private var triggered = false

    override fun check(delta: Float): Boolean {
        return triggered
    }

    override fun onAdded() {
        super.onAdded()

        playScreen.playStage.addOneTimeListener<NextTurnEvent> {
            if (nextPlayer == player) triggered = true

            return@addOneTimeListener triggered
        }
    }
}