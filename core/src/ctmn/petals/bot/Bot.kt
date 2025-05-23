package ctmn.petals.bot

import ctmn.petals.playscreen.PlayScreen
import ctmn.petals.player.Player
import ctmn.petals.playstage.PlayStage

abstract class Bot(val player: Player, val playScreen: PlayScreen) {

    val playerID get() = player.id

    var isDone: Boolean = false

    open fun levelCreated(playStage: PlayStage) {

    }

    /** Called by [BotManager] if [isDone] != true */
    abstract fun update(delta: Float)

    /** Called by [BotManager] on Bot's turn start */
    open fun onStart() {
        isDone = false
    }

    /** Called by [BotManager] on Bot's turn end */
    open fun onEnd() {
        isDone = true
    }

    /** Should be called by Bot when it's done */
    fun done() {
        isDone = true
    }
}