package ctmn.petals.multiplayer.json

import ctmn.petals.player.Player

class LobbyState {

    var level: String? = null

    val players = ArrayList<PlayerSlotState?>(8)

    var fogOfWar = true

    var state = State.WAITING

    enum class State {
        WAITING, STARTING, PLAYING, FINISHED, CLOSED
    }

    init {
        for (i in 0..7) {
            players.add(null)
        }
    }
}

class PlayerSlotState(
    val player: Player? = null,
    val isHost: Boolean = false,
    val isAI: Boolean = false
)