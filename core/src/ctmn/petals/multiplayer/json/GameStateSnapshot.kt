package ctmn.petals.multiplayer.json

import com.google.gson.JsonObject
import ctmn.petals.playscreen.PlayScreen
import ctmn.petals.playstage.getAllTilesAndUnits
import ctmn.petals.tile.TileActor
import ctmn.petals.unit.UnitActor

//todo copy
class GameStateSnapshot(playScreen: PlayScreen) {

    val localPlayer = playScreen.localPlayer

    val gameStateId = playScreen.gameStateId

    val players = playScreen.turnManager.players
    val aiPlayers = ArrayList<Int>().apply { playScreen.aiManager.aiPlayers.forEach { add(it.playerID) } }
    val turn = playScreen.turnManager.turn

    val gameEndCondition = playScreen.gameEndCondition.id

    val friendlyFire = playScreen.friendlyFire

    val isGameOver = playScreen.isGameOver

    val idCounter = playScreen.playStage.idCounter

    val randomSeed = playScreen.randomSeed
    val randomCount = playScreen.randomCount

    val tiles = mutableListOf<JsonObject>()
    val units = mutableListOf<JsonObject>()

    init {
        val actors = playScreen.playStage.getAllTilesAndUnits()
        for (actor in actors) {
            when (actor) {
                is TileActor -> {
                    tiles.add(actor.toJsonObject())
                }
                is UnitActor -> {
                    units.add(actor.toJsonObject())
                }
            }
        }
    }
}