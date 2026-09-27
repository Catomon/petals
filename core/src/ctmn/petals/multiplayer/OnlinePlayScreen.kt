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
import ctmn.petals.playscreen.events.CommandAddedEvent
import ctmn.petals.playscreen.events.CommandExecutedEvent
import ctmn.petals.playstage.getUnitsOfPlayer
import ctmn.petals.screens.PlayScreenTemplate
import ctmn.petals.utils.GameLog
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

    /** The server state each of our queued commands was built against. */
    private val commandStateIds = java.util.IdentityHashMap<Command, Int>()

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
                    fromGson(broadcast.commandJson, resolveCommandClass(broadcast.commandName)) as Command
                }.getOrNull() ?: return@onUi

                command.playerId = broadcast.playerId
                // Mark it as the server's, so applying it does not send it back as our own.
                command.fromServer = true

                when {
                    // Our own command that the server accepted: already applied locally.
                    broadcast.stateId == gameStateId -> Unit

                    // The next expected command, apply it now.
                    broadcast.stateId - 1 == gameStateId -> {
                        if (commandManager.queueCommand(command)) {
                            GameLog.verbose(
                                "OnlinePlayScreen",
                                "applying ${broadcast.commandName} from player ${broadcast.playerId}"
                            )
                        } else if (skipHiddenCommand(broadcast)) {
                            // The command only touches state we are not allowed to see, so we
                            // cannot replay it. Stepping the counter over it keeps the stream
                            // aligned, and the state we do not know about is exactly the state
                            // fog is supposed to hide. No full snapshot needed.
                            gameStateId = broadcast.stateId
                            GameLog.info(
                                "OnlinePlayScreen",
                                "skipped ${broadcast.commandName} from player ${broadcast.playerId}: hidden by fog"
                            )
                        } else {
                            requestResync(broadcast)
                        }
                    }

                    // More than one command behind: a real desync, so take the server state.
                    else -> requestResync(broadcast)
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

    /**
     * True when the other player has nothing of theirs in view, so a command of theirs that we
     * failed to replay is about hidden state rather than a real desync.
     */
    private fun skipHiddenCommand(broadcast: MatchCommandBroadcast): Boolean {
        if (broadcast.playerId == localPlayer.id) return false

        val theirPlayer = turnManager.getPlayerById(broadcast.playerId) ?: return false

        return playStage.getUnitsOfPlayer(theirPlayer).isEmpty()
    }

    private fun requestResync(broadcast: MatchCommandBroadcast) {
        GameLog.info(
            "OnlinePlayScreen",
            "gap: server state ${broadcast.stateId}, local $gameStateId, asking for a snapshot"
        )
        client.requestSnapshot()
    }

    fun start() {
        if (started) return
        started = true

        client.listener = listenerFor(client)
        clientControlled = true

        // The server owns the win conditions: this client only sees a filtered field.
        authoritativeGameEnd = false

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

    /**
     * Commands travel by class name. Newer peers send the full name, older ones only the
     * simple name, so both forms are accepted.
     */
    private fun resolveCommandClass(name: String): Class<*> {
        if (name.contains('.'))
            return Class.forName(name)

        return Class.forName("ctmn.petals.playscreen.commands.$name")
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
            when (it) {
                // The state a command is built against has to be remembered before the local
                // execution bumps the counter, otherwise the server sees it one ahead.
                is CommandAddedEvent -> if (it.command.playerId == localPlayer.id && !it.command.fromServer) {
                    commandStateIds[it.command] = gameStateId
                }

                is CommandExecutedEvent -> {
                    val command = it.command
                    if (command.playerId == localPlayer.id && !command.fromServer) {
                        val expectedStateId = commandStateIds.remove(command) ?: gameStateId - 1

                        GameLog.verbose(
                            "OnlinePlayScreen",
                            "sending ${command.javaClass.simpleName} (expected state $expectedStateId, local ${gameStateId})"
                        )

                        client.sendCommand(
                            requestId = "c$gameStateId",
                            expectedStateId = expectedStateId,
                            commandName = command.javaClass.name,
                            commandJson = command.toGson(),
                        )
                    }
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
