package ctmn.petals.multiplayer

import com.badlogic.gdx.Gdx
import com.badlogic.gdx.utils.Array
import ctmn.petals.GamePref
import ctmn.petals.map.createMapFromJson
import ctmn.petals.net.ServerClient
import ctmn.petals.net.proto.*
import ctmn.petals.player.Player
import ctmn.petals.playscreen.CaptureBases
import ctmn.petals.playscreen.GameMode
import ctmn.petals.playscreen.GameType
import ctmn.petals.playscreen.PlayScreen
import ctmn.petals.playscreen.Season
import ctmn.petals.playscreen.addAction
import ctmn.petals.playscreen.commands.Command
import ctmn.petals.playscreen.events.CommandExecutedEvent
import ctmn.petals.screens.PlayScreenTemplate
import ctmn.petals.utils.fromGson
import ctmn.petals.utils.toGson
import ctmn.petals.widgets.addNotifyWindow

/**
 * Plays a match hosted by the dedicated server.
 *
 * The client keeps its own renderable copy of the battle so the view, targeting and
 * animations work normally. Its own commands are applied optimistically and sent to the
 * server; every accepted command is broadcast to the match. A rejection or a state gap is
 * repaired with a fresh server snapshot.
 *
 * If the connection drops, the client reconnects with the same id so the server hands the
 * same seat back and sends a fresh snapshot.
 */
class OnlinePlayScreen(
    client: ServerClient,
    private val match: MatchFound,
    private val serverHost: String,
    private val serverPort: Int,
) : PlayScreen() {

    @Volatile
    private var client: ServerClient = client

    private var myPlayerId = match.you
    private var started = false
    private var leaving = false

    private var reconnecting = false
    private var reconnectAt = 0f
    private val reconnectDelay = 3f

    /**
     * One listener per connection. Callbacks from a connection that is no longer current
     * are ignored, so a stale socket can never drive the screen.
     */
    private fun listenerFor(owner: ServerClient) = object : ServerClient.Listener {

        private val current get() = client === owner && !leaving

        override fun onWelcome(welcome: Welcome) {
            if (!current) return
            onUi {
                // On a reconnect the server already knows our seat and pushes a snapshot.
                if (started) client.requestSnapshot()
            }
        }

        override fun onMatchSnapshot(snapshot: MatchSnapshot) {
            if (!current) return
            onUi {
                addAction {
                    val parsed = runCatching {
                        fromGson(
                            snapshot.snapshotJson,
                            ctmn.petals.multiplayer.json.GameStateSnapshot::class.java
                        )
                    }.getOrNull() ?: return@addAction

                    applyGameStateToPlayScreen(parsed, this@OnlinePlayScreen, isMultiplayer = true)
                }
            }
        }

        override fun onMatchCommand(broadcast: MatchCommandBroadcast) {
            if (!current) return
            onUi {
                val command = runCatching {
                    val clazz = Class.forName("ctmn.petals.playscreen.commands.${broadcast.commandName}")
                    fromGson(broadcast.commandJson, clazz) as Command
                }.getOrNull() ?: return@onUi

                command.playerId = broadcast.playerId

                when {
                    // Our own command that the server accepted: already applied locally.
                    broadcast.stateId == gameStateId -> Unit

                    // The next expected command, apply it now.
                    broadcast.stateId - 1 == gameStateId -> commandManager.queueCommand(command)

                    // We are out of sync, take the server state instead.
                    else -> client.requestSnapshot()
                }
            }
        }

        override fun onCommandRejected(rejected: CommandRejected) {
            if (!current) return
            onUi {
                guiStage.addNotifyWindow(rejected.reason.ifBlank { "Command rejected" }, "Server")
            }
        }

        override fun onMatchEnded(ended: MatchEnded) {
            if (!current) return
            onUi {
                if (isGameOver) return@onUi
                gameEndCondition.winners.clear()
                gameEndCondition.winners.addAll(ended.winners)
                gameOver()
            }
        }

        override fun onError(error: ErrorMsg) {
            if (!current) return
            onUi { guiStage.addNotifyWindow(error.message, "Server") }
        }

        override fun onConnectFailed(cause: Throwable?) {
            if (!current) return
            onUi { scheduleReconnect() }
        }

        override fun onDisconnected() {
            if (!current) return
            onUi {
                if (isGameOver || leaving) return@onUi
                guiStage.addNotifyWindow("Connection lost, reconnecting ...", "Multiplayer")
                scheduleReconnect()
            }
        }
    }

    fun start() {
        if (started) return
        started = true

        client.listener = listenerFor(client)
        clientControlled = true

        val map = createMapFromJson(match.mapJson)

        season = runCatching { Season.valueOf(match.season) }.getOrDefault(Season.SUMMER)

        val gameMode = runCatching { GameMode.valueOf(match.gameMode) }.getOrDefault(GameMode.CRYSTALS)

        val players = Array<Player>()
        match.players.forEach { info ->
            val player = Player(name = info.name, id = info.playerId, teamId = info.playerId)
            player.clientId = info.clientId
            players.add(player)
        }

        val me = players.firstOrNull { it.id == myPlayerId }

        PlayScreenTemplate.pvp(
            game,
            map,
            players,
            GameType.MULTIPLAYER,
            CaptureBases(),
            gameMode,
            me,
            this,
        )

        fogOfWarManager.drawFog = match.fogOfWar

        setDefaultPlayerIdToLabels()

        ready()
    }

    override fun show() {
        if (!started) start()
        super.show()
    }

    override fun update(delta: Float) {
        super.update(delta)

        if (!reconnecting || leaving) return

        reconnectAt -= delta
        if (reconnectAt > 0f) return

        reconnecting = false
        tryReconnect()
    }

    private fun scheduleReconnect() {
        if (reconnecting || leaving) return
        reconnecting = true
        reconnectAt = reconnectDelay
    }

    private fun tryReconnect() {
        val previous = client
        val fresh = ServerClient()
        fresh.listener = listenerFor(fresh)
        client = fresh

        fresh.connect(serverHost, serverPort, previous.clientId, GamePref.displayName)
    }

    override fun initGui() {
        super.initGui()

        // Send every command this player executes to the server.
        guiStage.addListener {
            if (it is CommandExecutedEvent) {
                val command = it.command
                if (command.playerId == localPlayer.id) {
                    client.sendCommand(
                        requestId = "c$gameStateId",
                        expectedStateId = gameStateId,
                        commandName = command.javaClass.simpleName,
                        commandJson = command.toGson(),
                    )
                }
            }

            false
        }
    }

    override fun dispose() {
        leaving = true
        client.close()
        super.dispose()
    }

    /** Network callbacks arrive on netty threads, so hop back to the render thread. */
    private fun onUi(block: () -> Unit) {
        Gdx.app.postRunnable(block)
    }
}
