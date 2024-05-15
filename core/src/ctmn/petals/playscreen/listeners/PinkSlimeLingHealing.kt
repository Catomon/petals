package ctmn.petals.playscreen.listeners

import com.badlogic.gdx.scenes.scene2d.Event
import com.badlogic.gdx.scenes.scene2d.EventListener
import ctmn.petals.playscreen.events.UnitDiedEvent
import ctmn.petals.playscreen.selfName
import ctmn.petals.unit.UnitIds
import ctmn.petals.unit.heal

class PinkSlimeLingHealing : EventListener {

    override fun handle(event: Event): Boolean {
        if (event is UnitDiedEvent) {
            if (event.unit.selfName == UnitIds.PINK_SLIME_LING && event.killer != event.unit) {
                event.killer?.heal(35)
            }
        }

        return false
    }
}