package ctmn.petals.multiplayer.json.serverres

import ctmn.petals.multiplayer.json.LobbyState

class LobbyStateResponse(val lobbyState: LobbyState) {
    val id = "lobby_state"
}