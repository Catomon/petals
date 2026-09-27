package ctmn.petals.multiplayer

import com.badlogic.gdx.Gdx
import com.badlogic.gdx.utils.Array
import ctmn.petals.GamePref
import ctmn.petals.map.createMapFromJson
import ctmn.petals.net.ServerClient
import ctmn.petals.net.SeatingLog
import ctmn.petals.net.proto.*
import ctmn.petals.player.Player
import ctmn.petals.playscreen.CaptureBases
import ctmn.petals.playscreen.GameMode
import ctmn.petals.playscreen.GameType
import ctmn.petals.playscreen.PlayScreen
import ctmn.petals.playscreen.Season
import ctmn.petals.playscreen.addAction
import ctmn.petals.playscreen.stageName
import ctmn.petals.playscreen.commands.Command
import ctmn.petals.playscreen.events.CommandAddedEvent
import ctmn.petals.playscreen.events.CommandExecutedEvent
import ctmn.petals.playscreen.commands.MoveUnitCommand
import ctmn.petals.playscreen.commands.TryMoveCommand
import ctmn.petals.playstage.findMovePath
import ctmn.petals.playstage.getUnitsOfPlayer
import ctmn.petals.screens.PlayScreenTemplate
import ctmn.petals.sim.FogOfWar
import ctmn.petals.unit.UnitActor
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
     * How far through the server's command stream we are, which is not the same thing as
     * [Battle.gameStateId].
     *
     * The game state counter only moves when a command finishes executing, and a command waits
     * its turn while the previous one's animation plays. Bots play faster than that queue drains,
     * so comparing the stream against the execution counter made a watching client look a command
     * behind and ask for a full snapshot every other command. This cursor moves as soon as a
     * command is accepted, so the stream stays in step while the animations catch up.
     *
     * -1 until the first snapshot arrives, which is the only point the server's numbering is known.
     */
    private var streamStateId = -1

    /**
     * False until the snapshot in [pendingSnapshot] has been written to the field. Commands that
     * arrive before then wait, because replaying one against a field the server has not described
     * yet fails its checks for the wrong reason.
     */
    private var snapshotApplied = false

    private var pendingSnapshot: MatchSnapshot? = null

    private val pendingCommands = ArrayList<MatchCommandBroadcast>()

    private companion object {
        /** How many commands to hold while waiting for the first snapshot before giving up on it. */
        const val MAX_PENDING_COMMANDS = 64
    }

    /**
     * Writes the newest snapshot we have been sent onto the field, then lets the commands that were
     * waiting for it through.
     *
     * The only place a snapshot is applied, so it cannot be applied twice or skipped. Safe to call
     * as often as needed: it does nothing once the held snapshot is in place.
     */
    private fun applyPendingSnapshot() {
        val snapshot = pendingSnapshot ?: return
        if (snapshotApplied) return

        val parsed = runCatching {
            fromGson(
                snapshot.snapshotJson,
                ctmn.petals.multiplayer.json.GameStateSnapshot::class.java
            )
        }.getOrNull()

        if (parsed == null) {
            // Leave the snapshot pending so the next command tries again, but do not sit on the
            // commands forever: ask for a fresh one.
            GameLog.error("OnlinePlayScreen", "could not read the match snapshot, asking for another")
            pendingSnapshot = null
            client.requestSnapshot()
            return
        }

        applyGameStateToPlayScreen(parsed, this, isMultiplayer = true)

        // A snapshot replaces the players with the server's copies, which carry the spectator
        // flag, but the GUI was built before any snapshot arrived. If this is the first one and
        // we are only watching, the end turn button goes away now rather than at match start.
        if (isSpectating) guiStage.endTurnButton.isVisible = false

        // A snapshot discards whatever was queued, so any command still waiting to be reported to
        // the server is moot and its bookkeeping goes with it.
        commandStateIds.clear()
        snapshotApplied = true

        GameLog.info(
            "OnlinePlayScreen",
            "applied the snapshot of match ${match.matchId} at state ${snapshot.stateId} " +
                    "(${parsed.units.size} unit(s), ${parsed.tiles.size} tile(s))"
        )

        val waiting = pendingCommands.toList()
        pendingCommands.clear()
        waiting.forEach { handleMatchCommand(it) }
    }

    /**
     * Replays one command from the server, or asks for the state when it cannot.
     *
     * Runs on the render thread. Must not be called before the first snapshot is in place: a
     * command replayed against a field the server has not described yet fails its own checks for
     * reasons that have nothing to do with the command, which used to cost a snapshot on the first
     * turn or two of every match.
     */
    private fun handleMatchCommand(broadcast: MatchCommandBroadcast) {
        // A command must never be replayed against a field the server has not described yet, so
        // make sure the snapshot we are holding is on the field first. Doing it here as well as in
        // the snapshot's own callback means a command can never be left waiting on an action that
        // never runs, which showed up as a match where simply nothing happened.
        if (!snapshotApplied) {
            if (pendingSnapshot != null) {
                applyPendingSnapshot()
            } else {
                pendingCommands.add(broadcast)

                // A snapshot that never turns up must not cost us memory forever. Past a sane
                // backlog the cheapest way out is to forget the queue and take the server's state.
                if (pendingCommands.size > MAX_PENDING_COMMANDS) {
                    GameLog.info(
                        "OnlinePlayScreen",
                        "no snapshot after ${pendingCommands.size} commands, asking for the state"
                    )
                    pendingCommands.clear()
                    pendingSnapshot = null
                    client.requestSnapshot()
                    return
                }

                GameLog.verbose(
                    "OnlinePlayScreen",
                    "holding ${broadcast.commandName} (state ${broadcast.stateId}) until the first snapshot arrives"
                )
                return
            }
        }

        val command = runCatching {
            fromGson(broadcast.commandJson, resolveCommandClass(broadcast.commandName)) as Command
        }.getOrNull() ?: return

        command.playerId = broadcast.playerId
        // Mark it as the server's, so applying it does not send it back as our own.
        command.fromServer = true

        when {
            // Our own command that the server accepted: already applied locally. This one has to be
            // judged against the execution counter, because we bumped it when we ran it ourselves.
            broadcast.stateId == gameStateId -> {
                streamStateId = broadcast.stateId
                Unit
            }

            // The next expected command, apply it now.
            broadcast.stateId - 1 == streamStateId -> {
                if (commandManager.queueCommand(command)) {
                    // Accepted, so the stream is in step. The execution counter catches up on its
                    // own once the command has played out.
                    streamStateId = broadcast.stateId
                    GameLog.verbose(
                        "OnlinePlayScreen",
                        "applying ${broadcast.commandName} from player ${broadcast.playerId}"
                    )
                } else if (skipHiddenCommand(broadcast)) {
                    // The command only touches state we are not allowed to see, so we cannot replay
                    // it. Stepping the cursor over it keeps the stream aligned, and the state we do
                    // not know about is exactly the state fog is supposed to hide.
                    streamStateId = broadcast.stateId
                    GameLog.info(
                        "OnlinePlayScreen",
                        "skipped ${broadcast.commandName} from player ${broadcast.playerId}: hidden by fog"
                    )
                } else {
                    // The command was the next one in the stream but would not run here. That is a
                    // real difference between our copy and the server's, not fog, so the state is
                    // the only way back into step.
                    requestResync(broadcast, "cannot replay ${broadcast.commandName}", command)
                }
            }

            // A gap: the server is further ahead than one command, so we missed something.
            else -> requestResync(broadcast, "gap in the command stream")
        }
    }

    /**
     * Rejects anything belonging to a different match.
     *
     * This connection is one socket, and a client can already be playing a new match while the
     * server is still finishing the old one. Without the check the two command streams arrive
     * interleaved: neither one lines up with the cursor, so every message looks like a gap, the
     * client asks for a snapshot per command, and snapshots of the new match land in the old one.
     */
    private fun isOurMatch(incoming: String, what: String): Boolean {
        if (incoming.isEmpty() || incoming == match.matchId) return true

        GameLog.info(
            "OnlinePlayScreen",
            "ignoring $what for match '$incoming': this screen is match '${match.matchId}'"
        )
        return false
    }

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
            if (!isOurMatch(snapshot.matchId, "snapshot")) return

            // The cursor moves the moment the snapshot lands, not when it is applied to the field.
            // Applying happens later on the render thread, and a command arriving in between would
            // otherwise find nothing to compare against and ask for a snapshot it does not need.
            streamStateId = snapshot.stateId

            pendingSnapshot = snapshot
            snapshotApplied = false

            onUi { applyPendingSnapshot() }
        }

        override fun onMatchCommand(broadcast: MatchCommandBroadcast) {
            if (!current) return
            if (!isOurMatch(broadcast.matchId, "command")) return
            onUi { handleMatchCommand(broadcast) }
        }

        override fun onCommandRejected(rejected: CommandRejected) {
            if (!current) return
            onUi {
                guiStage.addNotifyWindow(rejected.reason.ifBlank { "Command rejected" }, "Server")
            }
        }

        override fun onMatchEnded(ended: MatchEnded) {
            if (!current) return
            if (!isOurMatch(ended.matchId, "match ended")) return
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

        // A spectator is shown the whole map, so nothing is hidden from them and this test would
        // wrongly excuse every command we cannot replay. That froze a watching client's board:
        // the first bot buy arrived while that bot had no units yet, so it was skipped, and every
        // later command referred to a unit we had never seen. Without hidden state a failed replay
        // is a real desync, and a snapshot is the only honest way back into step.
        if (isSpectating) return false

        val theirPlayer = turnManager.getPlayerById(broadcast.playerId) ?: return false

        return playStage.getUnitsOfPlayer(theirPlayer).isEmpty()
    }

    private fun requestResync(broadcast: MatchCommandBroadcast, reason: String, command: Command? = null) {
        val because = command?.let { runCatching { it.explainFailure(this) }.getOrNull() }
            ?.let { ": $it" }
            ?: ""

        GameLog.info(
            "OnlinePlayScreen",
            "resync ($reason$because): ${broadcast.commandName} from p${broadcast.playerId}, " +
                    "server state ${broadcast.stateId}, stream $streamStateId, executed $gameStateId"
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
        // Order matters: the first player in this list goes first, so it must be slot order,
        // which is what the battle uses. Ids only carry the colour.
        val ordered = match.players.sortedWith(
            compareBy({ if (it.slot >= 0) 0 else 1 }, { it.slot }, { it.playerId })
        )
        ordered.forEach { info ->
            val player = Player(name = info.name, id = info.playerId, teamId = info.playerId)
            player.clientId = info.clientId
            // The host chose this species in the slot dialog, so keep it instead of letting the
            // default stand, otherwise a goblin turns into a fairy the moment the lobby opens.
            if (info.species.isNotBlank()) player.species = info.species
            // The server decided at seating time who only watches. The flag travels with the
            // player from here on, including inside snapshots, so the turn manager skips them.
            player.isSpectator = info.isSpectator
            players.add(player)
        }

        val me = players.firstOrNull { it.id == myPlayerId }

        GameLog.info(
            "OnlinePlayScreen",
            "entering match ${match.matchId}: " +
                    SeatingLog.describe(
                        "seats",
                        match.players.map {
                            SeatingLog.row(it.slot, it.playerId, it.name, it.isAi, it.botDifficulty)
                        },
                    ) + " | I am p$myPlayerId, first is p${players.firstOrNull()?.id}, " +
                    "this is my turn=${players.firstOrNull()?.id == myPlayerId}"
        )

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

        applyMapOwnersFromServer()

        ready()

        // A spectator has no turn to end, so the button is hidden rather than left greyed out for
        // the whole match. The turn banner still says what is happening.
        if (isSpectating) guiStage.endTurnButton.isVisible = false

        // The server sends the first snapshot immediately behind `match_found`, and that can land
        // before this screen's listener is installed above, in which case whoever was listening at
        // the time swallows it. Without a snapshot the client cannot tell a command from a gap, so
        // it quietly holds every command and the match plays out invisibly. Asking here makes the
        // start of a match independent of that race.
        if (!snapshotApplied) {
            GameLog.info(
                "OnlinePlayScreen",
                "no snapshot for match ${match.matchId} yet, asking for the state"
            )
            client.requestSnapshot()
        }
    }

    /**
     * The server seated everybody by lobby slot and said who owns which map place, so use that
     * instead of the local default of "the first place belongs to player one". Getting this wrong
     * gives a spectator a base and a vision bubble they never earned.
     */
    private fun applyMapOwnersFromServer() {
        val ownerBySlot = match.mapOwnerBySlot

        if (ownerBySlot.isEmpty()) {
            // No seating came from the server, for example a LAN host that sent none.
            setDefaultPlayerIdToLabels()
            return
        }

        val assigned = setMapOwnersFromSlots(ownerBySlot)

        GameLog.info(
            "OnlinePlayScreen",
            "map places from the server: " +
                    ownerBySlot.entries.sortedBy { it.key }
                        .joinToString(" ") { "place${it.key + 1}=>p${it.value}" } +
                    " | assigned $assigned place(s)"
        )
    }

    /**
     * Sends our move, optimistic when the route is fully visible, server verified otherwise.
     *
     * The server is only ever stricter where it sees enemies we don't: a hidden unit on a tile
     * blocks it, while one next to the route just makes it cheaper. So a route the viewer sees
     * in full gets the same verdict on both sides and stays instant. Anything else goes as a
     * try move, which the server resolves against the full state and broadcasts as the real
     * move, so both sides animate the same result.
     */
    override fun commitMove(unit: UnitActor, tileX: Int, tileY: Int): Boolean {
        if (!match.fogOfWar) return super.commitMove(unit, tileX, tileY)

        val command = MoveUnitCommand(unit, tileX, tileY)
        if (!command.canExecute(this)) return false

        val visibility = FogOfWar.compute(field, turnManager.players, localPlayer)
        val path = field.findMovePath(unit, tileX, tileY)
        val seen = path != null &&
                path.all { (x, y) -> FogOfWar.isVisible(visibility, x, y) }

        if (seen) return super.commitMove(unit, tileX, tileY)

        GameLog.info(
            "OnlinePlayScreen",
            "try-move ${unit.stageName} to $tileX,$tileY: the route is hidden by fog"
        )
        // No local apply, and no version check: the sender's executed count can lag behind the
        // server while animations play out, and legality is decided on arrival anyway.
        client.sendCommand(
            requestId = "t$gameStateId",
            expectedStateId = 0,
            commandName = TryMoveCommand::class.java.name,
            commandJson = TryMoveCommand(unit.stageName, tileX, tileY).toGson(),
        )
        return true
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

    /**
     * Quitting the match for good.
     *
     * This has to run on the way back to the menu and not only in [dispose], because swapping
     * `game.screen` does not dispose the screen it replaces. Without it the socket stayed open
     * and the server still considered this client a member, so the finished match kept sending
     * commands to somebody who had already started another one.
     */
    override fun onLeavingMatch() {
        if (leaving) return
        leaving = true

        // Best effort: if the socket is already gone there is nothing to tell the server.
        runCatching { client.leaveMatch() }
            .onFailure { GameLog.verbose("OnlinePlayScreen", "could not tell the server we left: ${it.message}") }

        GameLog.info(
            "OnlinePlayScreen",
            "left match ${match.matchId} as ${localPlayer.id} (spectating=$isSpectating)"
        )
    }

    override fun dispose() {
        onLeavingMatch()
        client.close()
        super.dispose()
    }

    /** Network callbacks arrive on netty threads, so hop back to the render thread. */
    private fun onUi(block: () -> Unit) {
        Gdx.app.postRunnable(block)
    }
}
