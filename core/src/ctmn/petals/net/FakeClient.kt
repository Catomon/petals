package ctmn.petals.net

import com.google.gson.JsonParser
import ctmn.petals.net.proto.*
import ctmn.petals.playscreen.commands.EndTurnCommand
import ctmn.petals.utils.GameLog
import ctmn.petals.utils.toGson

/**
 * Headless bot client used by the server load test. It connects, joins the quick match
 * queue and ends its turn whenever the server tells it that its turn started.
 */
class FakeClient(
    private val index: Int,
    host: String,
    port: Int,
    private val onMatchEnded: (FakeClient) -> Unit,
) {

    val client = ServerClient()

    var playerId = -1
        private set
    var matchId: String? = null
        private set
    var matches = 0
        private set

    private var greeted = false

    init {
        client.listener = object : ServerClient.Listener {
            override fun onWelcome(welcome: Welcome) {
                greeted = true
                client.queueJoin()
            }

            override fun onMatchFound(found: MatchFound) {
                playerId = found.you
                matchId = found.matchId
                matches++
            }

            override fun onMatchSnapshot(snapshot: MatchSnapshot) {
                if (isMyTurn(snapshot.snapshotJson)) {
                    client.sendCommand(
                        requestId = "turn-${snapshot.stateId}",
                        expectedStateId = snapshot.stateId,
                        commandName = "EndTurnCommand",
                        commandJson = EndTurnCommand(playerId).toGson(),
                    )
                }
            }

            override fun onCommandRejected(rejected: CommandRejected) {}

            override fun onMatchEnded(ended: MatchEnded) {
                matchId = null
                onMatchEnded(this@FakeClient)
                // Re-queue for another match while the load test is running.
                client.queueJoin()
            }

            override fun onError(error: ErrorMsg) {}

            override fun onDisconnected() {}
        }

        client.connect(host, port, "fake-$index", "Bot$index")
    }

    private fun isMyTurn(snapshotJson: String): Boolean {
        return try {
            val json = JsonParser.parseString(snapshotJson).asJsonObject
            val turn = json.get("turn").asInt
            val players = json.getAsJsonArray("players")
            if (turn < 0 || turn >= players.size()) return false
            players.get(turn).asJsonObject.get("id").asInt == playerId
        } catch (e: Exception) {
            false
        }
    }

    fun close() {
        client.close()
    }
}
