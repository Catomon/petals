package ctmn.petals.playscreen

import ctmn.petals.player.Player
import ctmn.petals.playscreen.events.NextRoundEvent
import ctmn.petals.playscreen.events.NextTurnEvent
import ctmn.petals.playscreen.listeners.TurnsCycleListener
import com.badlogic.gdx.utils.Logger
import java.lang.IllegalStateException

class TurnManager(val playScreen: PlayScreen) {

    val players = ArrayList<Player>()

    var round: Int = 0
    var turn: Int = 0

    private val turnCycleTime: Float get() = 1.toFloat() / players.size

    private val allPlayersOutOfGame: Boolean get() {
        for (player in players) {
            if (!player.isOutOfGame)
                return false
        }

        return true
    }

    var currentPlayer: Player
    get() = players[this.turn]
    /** before: [plA, plB, [value], plD] after: [[value], plD, plA, plB] */
    set(value) {
        if (!players.contains(value)) throw IllegalStateException("You should add player before doing this.")
        val dif = players.indexOf(value)
        val temp = ArrayList(players)
        for (pl in temp) {
            var newIndex = temp.indexOf(pl) - dif
            if (newIndex < 0)
                newIndex += players.size - 1
            players[newIndex] = pl
        }
    }

    val previousPlayer get() = if (turn - 1 >= 0) players[turn - 1] else players[players.size - 1]
    val nextPlayer get() = if (turn + 1 < players.size) players[turn + 1] else players[0]

    private val logger = Logger("TurnManager", Logger.INFO)

    fun nextTurn() {
        if (playScreen.isGameOver)
            return

        if (allPlayersOutOfGame)
            return

        //for event
        val lastPlayer = currentPlayer

        //change turn
        do {
            this.turn++
            if (this.turn == players.size) {
                this.turn = 0
                nextRound()
            }
            playScreen.fireEvent(TurnsCycleListener.TurnCycleEvent(lastPlayer, currentPlayer, turnCycleTime))
        } while (currentPlayer.isOutOfGame)

        //send event
        val nextPlayer = currentPlayer

        if (playScreen.gameType == GameType.PVP_SAME_SCREEN && !playScreen.aiManager.isAIPlayer(currentPlayer))
            playScreen.localPlayer = currentPlayer

        playScreen.fireEvent(NextTurnEvent(lastPlayer, nextPlayer))

        //log
        logger.info("Next Turn: ${currentPlayer.name}")
    }

    private fun nextRound() {
        //change round
        round++

        //send event
        playScreen.fireEvent(NextRoundEvent(round))

        //log
        logger.info("Next Round: ${round + 1}")
    }

    fun getPlayerById(id: Int): Player? {
        for (player in players) {
            if (player.id == id) return player
        }

        return null
    }
}
