package ctmn.petals.ai

import ctmn.petals.playscreen.PlayScreen
import ctmn.petals.player.Player

abstract class AIBot(val player: Player, val playScreen: PlayScreen) {

    val playerID get() = player.id

    var isDone: Boolean = false

    // AI manager will call it if isDone != true
    abstract fun update(delta: Float)

    // AI manager will call it at its player AI turn start
    open fun onStart() {
        isDone = false
    }

    // AI manager will call it at its player AI turn end
    open fun onEnd() {
        isDone = true
    }

    // AI should call it if it's done
    fun done() {
        isDone = true
    }
}