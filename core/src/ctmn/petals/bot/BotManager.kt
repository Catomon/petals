package ctmn.petals.bot

import ctmn.petals.player.Player
import ctmn.petals.playscreen.PlayScreen
import ctmn.petals.playscreen.events.NextTurnEvent
import com.badlogic.gdx.utils.Array
import ctmn.petals.bot.mediocre.MidBot
import ctmn.petals.multiplayer.ClientPlayScreen
import ctmn.petals.playscreen.commands.EndTurnCommand

class BotManager(val playScreen: PlayScreen) {

    val botPlayers = Array<Bot>()

    var current: Bot? = null

    //not working with more than 1 bot just delete
    //val isDone: Boolean get() { botPlayers.forEach { if (!it.isDone) return false }; return true}

    val isDone: Boolean get() = current?.isDone ?: true

    init {
        playScreen.playStage.addListener {
            if (it is NextTurnEvent) {
                current?.onEnd()

                for (bot in botPlayers) {
                    if (bot.playerID == it.nextPlayer.id) {
                        bot.onStart()
                        current = bot
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

        val current = current
        if (current != null) {
            if (!current.isDone) {
                current.update(delta)

                if (current.isDone)
                    playScreen.commandManager.queueCommand(EndTurnCommand(current.playerID))
            }
        }
    }

    fun add(botPlayer: Bot) {
        botPlayers.add(MidBot(botPlayer.player, botPlayer.playScreen))
    }

    fun isBotPlayer(player: Player): Boolean {
        botPlayers.forEach { if (it.playerID == player.id) return true }

        return false
    }
}