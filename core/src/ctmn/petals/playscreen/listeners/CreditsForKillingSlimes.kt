package ctmn.petals.playscreen.listeners

import com.badlogic.gdx.scenes.scene2d.Event
import com.badlogic.gdx.scenes.scene2d.EventListener
import ctmn.petals.Const.KILL_CREDITS_SLIME_HUGE
import ctmn.petals.Const.KILL_CREDITS_SLIME_LING
import ctmn.petals.playscreen.PlayScreen
import ctmn.petals.playscreen.events.UnitDiedEvent
import ctmn.petals.playscreen.gui.floatingLabel
import ctmn.petals.playscreen.selfName
import ctmn.petals.unit.UnitIds
import ctmn.petals.unit.playerId

class CreditsForKillingSlimes(val playScreen: PlayScreen) : EventListener {

    override fun handle(event: Event): Boolean {
        if (event is UnitDiedEvent) {
            if (event.unit.selfName == UnitIds.SLIME_HUGE || event.unit.selfName == UnitIds.SLIME_LING) {
                val killer = event.killer ?: return false
                val player = playScreen.turnManager.getPlayerById(killer.playerId) ?: return false
                val gold = if (event.unit.selfName == UnitIds.SLIME_HUGE) KILL_CREDITS_SLIME_HUGE else KILL_CREDITS_SLIME_LING
                player.gold += gold

                playScreen.guiStage.floatingLabel("${player.name} got $gold for killing a ${event.unit.selfName}")
            }
        }

        return false
    }
}