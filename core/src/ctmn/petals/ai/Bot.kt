package ctmn.petals.ai

import ctmn.petals.playscreen.PlayScreen
import ctmn.petals.player.Player

abstract class Bot(val player: Player, val playScreen: PlayScreen) {

    val playerID get() = player.id

    var isDone: Boolean = false

    /** update if [isDone] != true */
    abstract fun update(delta: Float)

    /** on Bot turn start */
    open fun onStart() {
        isDone = false
    }

    /** on Bot turn end */
    open fun onEnd() {
        isDone = true
    }

    /** called by Bot when it's done */
    fun done() {
        isDone = true
    }
}