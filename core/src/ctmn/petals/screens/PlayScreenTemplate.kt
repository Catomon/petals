package ctmn.petals.screens

import ctmn.petals.player.Player
import com.badlogic.gdx.utils.Array
import ctmn.petals.PetalsGame
import ctmn.petals.map.EXTRA_CREDITS_PER_BASE
import ctmn.petals.map.EXTRA_CREDITS_PER_CLUSTER
import ctmn.petals.map.MapConverted
import ctmn.petals.player.newBluePlayer
import ctmn.petals.player.newRedPlayer
import ctmn.petals.playscreen.*

object PlayScreenTemplate {

    fun test2Players(game: PetalsGame, map: MapConverted): PlayScreen {
        return pvp(
            game,
            map,
            players = Array<Player>().apply { add(newBluePlayer, newRedPlayer) },
            GameType.PVP_SAME_SCREEN,
            NoEnd(),
            GameMode.ALL
        ).apply {
            ready()
        }
    }

    fun pvp(
        game: PetalsGame,
        map: MapConverted,
        players: Array<Player>,
        gameType: GameType,
        gameEndCondition: GameEndCondition,
        gameMode: GameMode,
        pLocalPlayer: Player? = null,
        playScreen: PlayScreen = PlayScreen(game),
    ): PlayScreen {
        with(playScreen) {
            // Step 1: init map, add units and players, set up gameEndCondition
            setLevel(map)

            this.creditsPerBase = map.mapSave.extra?.get(EXTRA_CREDITS_PER_BASE) as Int? ?: this.creditsPerBase
            this.creditsPerCluster = map.mapSave.extra?.get(EXTRA_CREDITS_PER_CLUSTER) as Int? ?: this.creditsPerCluster

            this.gameType = gameType
            this.gameMode = gameMode

            turnManager.players.addAll(players)
            turnManager.currentPlayer =
                    /** player ?: */
                players.first()

            this.gameEndCondition = gameEndCondition

            // Step 1.5: set localPlayer
            localPlayer = if (gameType == GameType.PVP_SAME_SCREEN)
                if (botManager.isBotPlayer(turnManager.currentPlayer)) turnManager.players.first {
                    !botManager.isBotPlayer(it)
                } else turnManager.currentPlayer
            else
                pLocalPlayer ?: turnManager.players.first { !botManager.isBotPlayer(it) }

            // Step 2: initGui
            //if player == null, init with first non-ai player

            // Step 3: make tasks, triggers etc.

            //...

            return this
        }
    }
}