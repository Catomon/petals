package ctmn.petals

import ctmn.petals.level.Level
import ctmn.petals.player.Player
import com.badlogic.gdx.utils.Array
import ctmn.petals.player.newBluePlayer
import ctmn.petals.player.newRedPlayer
import ctmn.petals.playscreen.*

object PlayScreenTemplate {

    fun test2Players(game: TTPGame, level: Level): PlayScreen {
        return pvp(
            game,
            level,
            players = Array<Player>().apply { add(newBluePlayer, newRedPlayer) },
            GameType.PVP_SAME_SCREEN,
            NoEnd(),
            GameMode.ALL
        ).apply {
            ready()
        }
    }

    fun pvp(
        game: TTPGame,
        level: Level,
        players: Array<Player>,
        gameType: GameType,
        gameEndCondition: GameEndCondition,
        gameMode: GameMode,
        pLocalPlayer: Player? = null,
        playScreen: PlayScreen = PlayScreen(game)
    ): PlayScreen {
        with (playScreen) {
            // Step 1: init map, add units and players, set up gameEndCondition
            setLevel(level)

            this.gameType = gameType
            this.gameMode = gameMode

            turnManager.players.addAll(players)
            turnManager.currentPlayer =
                    /** player ?: */
                players.first()

            this.gameEndCondition = gameEndCondition

            // Step 1.5: set localPlayer
            localPlayer = pLocalPlayer ?: turnManager.players.first { !aiManager.isAIPlayer(it) }

            // Step 2: initGui
            //if player == null, init with first non-ai player

            // Step 3: make tasks, triggers etc.

            //...

            return this
        }
    }
}