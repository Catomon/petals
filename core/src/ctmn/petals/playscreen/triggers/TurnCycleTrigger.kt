package ctmn.petals.playscreen.triggers

import ctmn.petals.player.Player
import ctmn.petals.playscreen.listeners.TurnsCycleListener

class TurnCycleTrigger(turnCycles: Int, val player: Player? = null) : Trigger() {

    val listener: TurnsCycleListener

    var triggered = false

    init {
        listener =
            if (player != null)
                TurnsCycleListener(player.id, turnCycles)
            else
                TurnsCycleListener(turnCycles)

        listener.action = {
            triggered = true
        }
    }

    override fun check(delta: Float): Boolean = triggered

    override fun onAdded() {
        playScreen.playStage.addListener(listener)
    }
}