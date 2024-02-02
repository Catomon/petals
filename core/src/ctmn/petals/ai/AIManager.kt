package ctmn.petals.ai

import ctmn.petals.player.Player
import ctmn.petals.playscreen.PlayScreen
import ctmn.petals.playscreen.events.NextTurnEvent
import com.badlogic.gdx.utils.Array
import ctmn.petals.multiplayer.ClientPlayScreen

class AIManager(val playScreen: PlayScreen) {

    val aiPlayers = Array<AIBot>()

    var current: AIBot? = null

    //not working with more than 1 ai just delete
    //val isDone: Boolean get() { aiPlayers.forEach { if (!it.isDone) return false }; return true}

    val isDone: Boolean get() = current?.isDone ?: true

    init {
        playScreen.playStage.addListener {
            if (it is NextTurnEvent) {
                current?.onEnd()

                for (ip in aiPlayers) {
                    if (ip.playerID == it.nextPlayer.id) {
                        ip.onStart()
                        current = ip
                        return@addListener false
                    }
                }

                current = null
            }


            false
        }
    }

    fun update(delta: Float) {
        if (playScreen is ClientPlayScreen) return

        if (current?.isDone != true) {
            current?.update(delta)

            if (current?.isDone == true)
                playScreen.turnManager.nextTurn()
        }
    }

    fun add(aiPlayer: AIBot) {
        aiPlayers.add(aiPlayer)
    }

    fun isAIPlayer(player: Player) : Boolean {
        aiPlayers.forEach { if (it.playerID == player.id) return true }

        return false
    }
}