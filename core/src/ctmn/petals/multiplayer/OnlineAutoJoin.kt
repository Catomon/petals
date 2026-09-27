package ctmn.petals.multiplayer

import com.badlogic.gdx.Gdx
import ctmn.petals.GamePref
import ctmn.petals.PetalsGame
import ctmn.petals.net.ServerClient
import ctmn.petals.net.proto.ErrorMsg
import ctmn.petals.net.proto.MatchFound
import ctmn.petals.net.proto.Welcome
import ctmn.petals.utils.GameLog
import java.util.UUID

/**
 * Smoke test helper: connects to a dedicated server and joins the quick match queue without
 * going through the menus. Used to verify the online flow from the command line with
 * `.\gradlew.bat :desktop:run --args="onlinetest localhost 7396"`.
 */
object OnlineAutoJoin {

    fun start(game: PetalsGame, host: String, port: Int) {
        GameLog.info("OnlineAutoJoin", "Connecting to $host:$port")

        val client = ServerClient()
        // A unique id per run, so several clients on one machine do not look like a reconnect.
        val clientId = "${GamePref.clientId}-${UUID.randomUUID().toString().take(8)}"

        client.listener = object : ServerClient.Listener {

            override fun onWelcome(welcome: Welcome) {
                GameLog.info("OnlineAutoJoin", "Connected to '${welcome.serverName}', joining the queue")
                client.queueJoin()
            }

            override fun onMatchFound(found: MatchFound) {
                GameLog.info("OnlineAutoJoin", "Matched into ${found.matchId} as player ${found.you} on ${found.mapId}")

                Gdx.app.postRunnable {
                    game.screen = OnlinePlayScreen(client, found, host, port)
                }
            }

            override fun onError(error: ErrorMsg) {
                GameLog.error("OnlineAutoJoin", "server said: ${error.message}")
            }

            override fun onConnectFailed(cause: Throwable?) {
                GameLog.error("OnlineAutoJoin", "could not connect to $host:$port", cause ?: RuntimeException("unknown"))
            }
        }

        client.connect(host, port, clientId, GamePref.displayName)
    }
}
