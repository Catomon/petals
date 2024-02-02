package ctmn.petals.multiplayer.json.serverres

import ctmn.petals.multiplayer.json.GameStateSnapshot

class GameStateResponse(val gameState: GameStateSnapshot) {
    val id = "game_state"
}