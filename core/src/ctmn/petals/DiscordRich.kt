package ctmn.petals

import net.arikia.dev.drpc.DiscordEventHandlers
import net.arikia.dev.drpc.DiscordRPC
import net.arikia.dev.drpc.DiscordRichPresence
import java.util.Scanner
import kotlin.concurrent.thread

fun startDiscordRich() {
    DiscordRPC.discordInitialize("1209175690793066516", DiscordEventHandlers(), true)

    val presence = DiscordRichPresence().apply {

    }
//        DiscordRichPresence.Builder("current state")
//            .setBigImage("big_icon", GameConst.APP_NAME)
//            .setStartTimestamps(System.currentTimeMillis() / 1000L)
//            .build()

    DiscordRPC.discordUpdatePresence(presence)
}

fun stopDiscordRich() {
    DiscordRPC.discordShutdown()
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