package ctmn.petals.playscreen.triggers

import ctmn.petals.playscreen.seqactions.SeqAction

class OnActionCompleteTrigger(val action: SeqAction) : Trigger() {
    override fun check(delta: Float): Boolean {
        return action.isDone
    }
}