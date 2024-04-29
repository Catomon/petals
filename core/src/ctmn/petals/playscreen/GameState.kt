package ctmn.petals.playscreen

import ctmn.petals.player.Player
import ctmn.petals.playstage.getTiles
import ctmn.petals.playstage.getUnits
import ctmn.petals.tile.TileActor
import ctmn.petals.unit.UnitActor

class GameState(playScreen: PlayScreen) {

    val localPlayer = playScreen.localPlayer.copy()

    val gameStateId = playScreen.gameStateId

    val players = ArrayList<Player>().apply { playScreen.turnManager.players.forEach { add(it.copy()) } }
    val aiPlayers = ArrayList<Int>().apply { playScreen.botManager.botPlayers.forEach { add(it.playerID) } }
    val turn = playScreen.turnManager.turn

    val friendlyFire = playScreen.friendlyFire

    val isGameOver = playScreen.isGameOver

    val idCounter = playScreen.playStage.idCounter

    val randomSeed = playScreen.randomSeed
    val randomCount = playScreen.randomCount

    val tiles = mutableListOf<TileActor>().apply {
        playScreen.playStage.getTiles().forEach { add(it.makeCopy()) }
    }
    val units = mutableListOf<UnitActor>().apply {
        playScreen.playStage.getUnits().forEach { add(it.makeCopy()) }
    }
}