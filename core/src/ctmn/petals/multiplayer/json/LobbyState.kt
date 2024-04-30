package ctmn.petals.multiplayer.json

import ctmn.petals.player.Player
import ctmn.petals.playscreen.GameMode
import ctmn.petals.playstage.PlayStage

class LobbyState {

    var mapId: String? = null

    val players = ArrayList<PlayerSlotState?>(8)

    var fogOfWar = true

    var daytime = PlayStage.DayTime.DAY

    var gameMode = GameMode.CRYSTALS

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