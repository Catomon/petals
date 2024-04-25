package ctmn.petals

import net.arikia.dev.drpc.DiscordEventHandlers
import net.arikia.dev.drpc.DiscordRPC
import net.arikia.dev.drpc.DiscordRichPresence
import java.util.Scanner
import kotlin.concurrent.thread

var discordRichDisabled = !Const.IS_DESKTOP || Const.DISABLE_RICH

fun startDiscordRich() {
    if (discordRichDisabled) return

    DiscordRPC.discordInitialize("1209175690793066516", DiscordEventHandlers(), true)
}

fun stopDiscordRich() {
    if (discordRichDisabled) return

    DiscordRPC.discordShutdown()
}

enum class Rich {
    DEFAULT,
    EDITOR,
    PLAYING,
}

fun discordRich(rich: Rich) {
    if (discordRichDisabled) return

    val presence = DiscordRichPresence().apply {
        largeImageKey = "big_icon"
        largeImageText = Const.APP_NAME

        when (rich) {
            Rich.DEFAULT -> {

            }
            Rich.EDITOR -> {
                smallImageKey = "small_icon"
                smallImageText = "Petals"
                details = "In Map Editor"
            }
            Rich.PLAYING -> {
                smallImageKey = "small_icon"
                smallImageText = "Petals"
                details = "Playing"
            }
        }
    }

    DiscordRPC.discordUpdatePresence(presence)
}

fun main() {
    DiscordRPC.discordInitialize("1209175690793066516", DiscordEventHandlers(), true)

    val presence =
        DiscordRichPresence.Builder("current state")
            .setBigImage("big_icon", "big")
            .setSmallImage("small_icon", "small")
            .setDetails("details")
            .build()

    DiscordRPC.discordUpdatePresence(presence)

    Thread.sleep(3000)

    presence.details = "updated details"
    presence.endTimestamp = System.currentTimeMillis() / 1000L + 300L
    presence.startTimestamp = System.currentTimeMillis() / 1000L

    DiscordRPC.discordUpdatePresence(presence)

    val t = thread {
        presence.details = Scanner(System.`in`).nextLine()
        DiscordRPC.discordUpdatePresence(presence)
    }

    var currentSec = 0
    val maxSec = 300
    while (currentSec < maxSec) {
        currentSec++
        Thread.sleep(1000)
        //DiscordRPC.discordUpdatePresence(presence)
    }

    Thread.sleep(5000)

    t.interrupt()
    DiscordRPC.discordShutdown()
}