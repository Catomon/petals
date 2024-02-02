package ctmn.petals.effects

import ctmn.petals.playscreen.PlayScreen
import ctmn.petals.playscreen.triggers.Trigger

class EffectTrigger : Trigger() {
    override var onTrigger: ((PlayScreen) -> Unit)?
        get() = super.onTrigger
        set(value) {}

    override fun check(delta: Float): Boolean {
        TODO("Not yet implemented")
    }

    override fun onAdded() {
        super.onAdded()
    }
}