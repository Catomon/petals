package ctmn.petals.playscreen.triggers

import ctmn.petals.playscreen.PlayScreen

fun turnCycleTrigger(turn: Int, onTrigger: (PlayScreen) -> Unit): TurnCycleTrigger {
    val trigger = TurnCycleTrigger(turn)
    trigger.onTrigger = onTrigger
    return trigger
}