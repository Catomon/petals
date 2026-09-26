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

        battle.setupSpawnPoints()
        battle.startFirstTurn()

        return battle
    }
}
