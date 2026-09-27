package ctmn.petals.sim

import ctmn.petals.map.MapConverted
import ctmn.petals.player.Player
import ctmn.petals.playscreen.CaptureBases
import ctmn.petals.playscreen.GameEndCondition
import ctmn.petals.playscreen.GameMode
import ctmn.petals.playscreen.GameType
import ctmn.petals.playscreen.Season

/**
 * Builds a fully set up headless battle from a map and players, mirroring what
 * `PlayScreenTemplate.pvp` plus `PlayScreen.ready` do for the client.
 */
object BattleSetup {

    fun build(
        map: MapConverted,
        players: List<Player>,
        gameType: GameType = GameType.MULTIPLAYER,
        gameMode: GameMode = GameMode.CRYSTALS,
        gameEndCondition: GameEndCondition = CaptureBases(),
        season: Season = Season.SUMMER,
        friendlyFire: Boolean = false,
        turnTimeLimitSeconds: Int = 0,
        /**
         * Which player owns which map place, by the lobby slot they sit in. A map place with no
         * player in the matching slot stays unowned, which is how a spectator works. When this is
         * empty the map's own player labels decide, as the local flow does.
         */
        playerIdBySlot: Map<Int, Int> = emptyMap(),
    ): ServerBattle {
        require(players.isNotEmpty()) { "A battle needs at least one player" }

        val battle = ServerBattle()
        battle.gameType = gameType
        battle.gameMode = gameMode
        battle.gameEndCondition = gameEndCondition
        battle.season = season
        battle.friendlyFire = friendlyFire
        battle.turnTimeLimitSeconds = turnTimeLimitSeconds

        battle.loadMap(map)

        battle.turnManager.players.addAll(players)
        battle.turnManager.currentPlayer = players.first()
        battle.localPlayer = players.first()

        if (playerIdBySlot.isEmpty()) {
            battle.setupDefaultPlayerIds()
        } else {
            battle.assignMapPlacesBySlot(playerIdBySlot)
        }

        battle.setupSpawnPoints()
        battle.startFirstTurn()

        return battle
    }
}
