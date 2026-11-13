package ctmn.petals

/**
 * What the game is doing, for platforms that can show it outside the window.
 * Desktop installs a Discord implementation, other targets keep the hook empty.
 */
enum class Rich {
    DEFAULT,
    EDITOR,
    PLAYING,
}

interface DiscordPresence {
    fun start()
    fun stop()
    fun update(rich: Rich)
}

var discordPresence: DiscordPresence? = null

fun startDiscordRich() {
    discordPresence?.start()
}

fun stopDiscordRich() {
    discordPresence?.stop()
}

fun discordRich(rich: Rich) {
    discordPresence?.update(rich)
}
