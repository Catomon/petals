package ctmn.petals.multiplayer.json

import com.google.gson.JsonObject
import ctmn.petals.playstage.getAllTilesAndUnits
import ctmn.petals.sim.Battle
import ctmn.petals.tile.TileActor
import ctmn.petals.unit.UnitActor

class GameStateSnapshot(battle: Battle) {

    val localPlayer = battle.localPlayer

    val gameStateId = battle.gameStateId

    val players = battle.turnManager.players.map { it }
    val aiPlayers = ArrayList<Int>().apply { battle.botManager.botPlayers.forEach { add(it.playerID) } }
    val turn = battle.turnManager.turn

    val gameEndCondition = battle.gameEndCondition.id

    val friendlyFire = battle.friendlyFire

    val isGameOver = battle.isGameOver

    val idCounter = battle.field.idCounter

    val randomSeed = battle.randomSeed
    val randomCount = battle.randomCount

    val tiles = mutableListOf<JsonObject>()
    val units = mutableListOf<JsonObject>()

    init {
        val actors = battle.field.getAllTilesAndUnits()
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
