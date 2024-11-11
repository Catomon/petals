package ctmn.petals.playscreen.events

import ctmn.petals.player.Player

class CreditsChangeEvent(val player: Player, val amount: Int) : PlayStageEvent()
