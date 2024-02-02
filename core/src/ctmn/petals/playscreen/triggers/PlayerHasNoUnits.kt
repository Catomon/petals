package ctmn.petals.playscreen.triggers

import ctmn.petals.player.Player
import ctmn.petals.playstage.getUnitsOfPlayer

class PlayerHasNoUnits(val player: Player) : Trigger() {

    override fun check(delta: Float): Boolean {
        return playScreen.playStage.getUnitsOfPlayer(player).isEmpty
    }
}