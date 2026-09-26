package ctmn.petals.bot

import ctmn.petals.player.Player
import ctmn.petals.sim.Battle
import ctmn.petals.sim.BattleField

abstract class Bot(val player: Player, val battle: Battle) {

    val playerID get() = player.id

    var isDone: Boolean = false

    open fun levelCreated(field: BattleField) {

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