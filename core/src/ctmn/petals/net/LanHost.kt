package ctmn.petals.net

import ctmn.petals.Config
import ctmn.petals.GamePref
import ctmn.petals.server.ServerConfig
import ctmn.petals.utils.GameLog

/**
 * The server a LAN host runs inside their own game.
 *
 * A LAN room is not a separate kind of room: it is a normal [ServerCore] on the LAN port, and the
 * host is a normal client of it. That is what lets LAN reuse [ctmn.petals.menu.ServerLobbySetupStage]
 * and [ctmn.petals.multiplayer.OnlinePlayScreen] unchanged, so seating, species, disconnect
 * handling and the match itself behave the way they do online.
 *
 * One room at a time. [start] replaces any previous one, and [stop] is what keeps the room's
 * threads from outliving the window.
 */
object LanHost {

    @Volatile
    private var core: ServerCore? = null

    val isRunning: Boolean get() = core != null

    /** Starts a fresh room on [Config.LAN_PORT]. Stops the previous one first, if any. */
    fun start(): ServerCore {
        stop()

        val config = ServerConfig().apply {
            port = Config.LAN_PORT
            serverName = "${GamePref.displayName}'s room"
            // A LAN room is one lobby and one match, so the server stays small and predictable.
            maxSessions = 4
            lanDiscovery = true
        }

        val started = ServerCore(config)
        started.start()
        core = started

        GameLog.info("LanHost", "hosting a LAN room on port ${Config.LAN_PORT}")
        return started
    }

    fun stop() {
        core?.stop()
        core = null
    }
}
