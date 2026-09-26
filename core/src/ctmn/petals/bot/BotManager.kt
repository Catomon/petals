package ctmn.petals.bot

import com.badlogic.gdx.utils.Array
import ctmn.petals.player.Player
import ctmn.petals.playscreen.commands.EndTurnCommand
import ctmn.petals.playscreen.events.NextTurnEvent
import ctmn.petals.sim.Battle

class BotManager(val battle: Battle) {

    val botPlayers = Array<Bot>()

    var current: Bot? = null

    val isDone: Boolean get() = current?.isDone ?: true

    init {
        battle.addEventListener {
            if (it is NextTurnEvent) {
                current?.onEnd()

                for (bot in botPlayers) {
                    if (bot.playerID == it.nextPlayer.id) {
                        bot.onStart()
                        current = bot
                        return@addEventListener false
                    }
                }

                current = null
            }

            false
        }
    }

    fun update(delta: Float) {
        if (battle.clientControlled) return

        val current = current
        if (current != null) {
            if (!current.isDone) {
                current.update(delta)

                if (current.isDone)
                    battle.commandManager.queueCommand(EndTurnCommand(current.playerID), current.playerID)
            }
        }
    }

    fun add(bot: Bot) {
        botPlayers.add(bot)
    }

    fun isBotPlayer(player: Player): Boolean {
        botPlayers.forEach { if (it.playerID == player.id) return true }

        return false
    }
}
