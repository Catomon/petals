package ctmn.petals.playscreen.events

import ctmn.petals.player.Player

class NextTurnEvent(val previousPlayer: Player, val nextPlayer: Player) : PlayStageEvent()
