package ctmn.petals.menu

import com.badlogic.gdx.Gdx
import com.badlogic.gdx.scenes.scene2d.Actor
import com.badlogic.gdx.scenes.scene2d.Stage
import com.kotcrab.vis.ui.widget.VisTable
import com.kotcrab.vis.ui.widget.VisWindow
import ctmn.petals.GamePref
import ctmn.petals.map.MapConverted
import ctmn.petals.map.createMapFromJson
import ctmn.petals.map.loadMap
import ctmn.petals.playscreen.GameMode
import ctmn.petals.playscreen.Season
import ctmn.petals.playstage.PlayStage
import ctmn.petals.multiplayer.OnlinePlayScreen
import ctmn.petals.net.ServerClient
import ctmn.petals.net.SeatingLog
import ctmn.petals.net.proto.CreateLobby
import ctmn.petals.net.proto.MatchSnapshot
import ctmn.petals.net.proto.ErrorMsg
import ctmn.petals.net.proto.LobbyMember
import ctmn.petals.net.proto.LobbySeat
import ctmn.petals.net.proto.LobbyStateMsg
import ctmn.petals.net.proto.MatchFound
import ctmn.petals.player.Player
import ctmn.petals.player.Species
import ctmn.petals.screens.MenuScreen
import ctmn.petals.strings
import ctmn.petals.utils.GameLog
import ctmn.petals.utils.toGson
import ctmn.petals.widgets.addChangeListener
import ctmn.petals.widgets.addNotifyWindow
import ctmn.petals.widgets.newLabel
import ctmn.petals.widgets.newTextButton

/**
 * Sets up a match on the dedicated server, using the same [GameSetupPane] as the local flow.
 *
 * Reached from the lobby browser either to create a lobby or to open one. Bot seats are chosen
 * here, and only the host can start the match. When the server finds a match it hands over to
 * [OnlinePlayScreen].
 */
class ServerLobbySetupStage(
    private val menuScreen: MenuScreen,
    private val client: ServerClient,
    private val serverHost: String,
    private val serverPort: Int,
    /** Null when this screen creates a new lobby rather than opening an existing one. */
    private val lobbyId: String?,
) : Stage(menuScreen.viewport, menuScreen.batch) {

    private val pane = GameSetupPane(this)

    private val returnButton = newTextButton(strings.general.return_)

    /**
     * Publishes the lobby so it appears in the browser, or unpublishes it again. The lobby itself
     * already exists by the time this screen opens.
     */
    private val openLobbyButton = newTextButton("Open").apply { isDisabled = true }

    /** Runs the match the lobby is waiting for. Host only, and only with somebody to play. */
    private val startButton = newTextButton("Start").apply { isDisabled = true }

    /** Wraps: the status sentences are longer than the pane's 300px column. */
    private val statusLabel = newLabel("", "default").apply { setWrap(true) }

    private val table = VisTable()
    private val windowTable = VisTable()

    /** Set once the server confirmed our lobby, so the buttons reflect a real seat. */
    private var inLobby = false

    /** Whether the lobby is currently in the browser. Mirrors what the server last told us. */
    private var listed = false

    /**
     * Creating a lobby means we are the host by definition. The server confirms it later, but
     * the confirm button must be live before that, or a host can never start their own lobby.
     */
    private var isHost: Boolean = lobbyId == null
    private var leaving = false
    private var chosenMapId: String? = null

    /** Set when the chosen map came from this machine and must be sent to the server. */
    private var chosenMapJson: String? = null

    /**
     * True while an incoming lobby state is being written into the pane.
     *
     * Writing into the pane fires [GameSetupPane.onLobbyChanged], which would send the same
     * state straight back to the server, which would answer with the same state again. That is a
     * loop at wire speed: dozens of updates a second, the map and settings never settling, and
     * every other player's species rerolled on each pass.
     */
    private var applyingRemoteState = false

    /**
     * Our own seat, kept as one object for the whole screen. The server sends us a plain
     * player for our own member entry, and swapping that in would lose the local player mark
     * and break "move here".
     */
    private val myPlayer = Player(
        name = GamePref.displayName.ifBlank { "Player" },
        id = 1,
        teamId = 1,
    ).apply { clientId = GamePref.clientId }

    private val listener = object : ServerClient.Listener {

        override fun onLobbyState(state: LobbyStateMsg) {
            onUi {
                // Creating a lobby has no id yet, so accept the first state we are told about.
                if (lobbyId != null && state.id != lobbyId) return@onUi

                // Everything below writes into the pane, and writing into the pane reports a local
                // change. Reporting those back would be a loop, so the echo is muted while the
                // server's own answer is being applied.
                applyingRemoteState = true
                try {
                    applyLobbyState(state)
                } finally {
                    applyingRemoteState = false
                }

                refresh()

                GameLog.verbose(
                    "ServerLobbySetupStage",
                    "lobby state: " + SeatingLog.describe("seats", state.members.toList()) +
                            " host=${state.hostClientId == GamePref.clientId}"
                )
            }
        }

        /**
         * The server sends the first snapshot right behind `match_found`, so it can reach us before
         * the play screen has taken over as the listener. It would be dropped in silence, and the
         * play screen would then hold every command waiting for a state that never comes.
         */
        override fun onMatchSnapshot(snapshot: MatchSnapshot) {
            GameLog.info(
                "ServerLobbySetupStage",
                "a match snapshot (state ${snapshot.stateId}) arrived while still in the lobby; " +
                        "the play screen asks for its own"
            )
        }

        override fun onMatchFound(found: MatchFound) {
            onUi {
                leaving = true
                menuScreen.game.screen = OnlinePlayScreen(client, found, serverHost, serverPort)
            }
        }

        override fun onError(error: ErrorMsg) {
            onUi { addNotifyWindow(error.message, "Server") }
        }

        override fun onDisconnected() {
            onUi {
                if (leaving || menuScreen.game.screen is OnlinePlayScreen) return@onUi
                addNotifyWindow("Lost connection to the server", "Lobby")
                back()
            }
        }
    }

    init {
        pane.allowLocalPlayer = false
        // Once the lobby exists every change goes straight back to the server, so the players
        // already sitting in it see the same arrangement the host is looking at. A lobby with no
        // map yet has nothing to send, and createLobby says so through the status line.
        pane.onLobbyChanged = {
            if (!applyingRemoteState) createLobby()
        }
        pane.onUnplayableMap = { message -> addNotifyWindow(message, "Lobby") }

        pane.changeMapButton.addChangeListener { showMapPicker() }

        returnButton.addChangeListener { back() }

        // Publishing and starting are separate, and neither is "create": the lobby already exists
        // by the time this screen is open.
        openLobbyButton.addChangeListener {
            listed = !listed
            createLobby()
            refresh()
        }
        startButton.addChangeListener { startMatch() }

        // The pane's column goes straight on the stage, exactly as the local flow lays it out.
        // Nesting it inside a centred table puts it in the wrong corner, because the pane fills
        // its parent.
        addActor(pane.content)

        with(pane.content) {
            row()
            add(VisTable().apply {
                width = 300f
                add(returnButton).left()
                add().expandX()

                // The host's two actions sit next to each other, in the order they are used:
                // publish the lobby, then run the match that fills it.
                add(openLobbyButton).width(100f)
                add(startButton).width(100f).padLeft(4f)
            }).width(300f)
            row()
            add(statusLabel).width(300f).padTop(4f)
        }

        table.setFillParent(true)
        table.center()

        windowTable.setFillParent(true)
        addActor(windowTable)

        client.listener = listener

        refresh()

        if (lobbyId != null) {
            // Our own species travels with the join, otherwise the host's lobby has nothing to
            // show for us and every update rerolls it.
            client.joinLobby(lobbyId, myPlayer.species)
        } else {
            // Creating: sit in the first slot straight away, as the local flow does, and open the
            // lobby straight away too. It stays private until the host publishes it, so the
            // arrangement can be built in peace and appears in the browser when it is ready.
            pane.localPlayer = myPlayer
            pane.setPlayerSlot(myPlayer, 0)
            createLobby()
        }
    }

    /** Writes the server's view of the lobby into the pane. Never reports a local change. */
    private fun applyLobbyState(state: LobbyStateMsg) {
        inLobby = true
        isHost = state.hostClientId == GamePref.clientId
        pane.isHost = isHost

        if (state.mapId != null && state.mapId != chosenMapId) {
            showChosenMap(state.mapId, state.mapJson.ifBlank { null })
        }
        pane.fogOfWar = state.fogOfWar
        pane.season = runCatching { Season.valueOf(state.season) }.getOrDefault(Season.SUMMER)
        pane.daytime = runCatching { PlayStage.DayTime.valueOf(state.daytime) }
            .getOrDefault(PlayStage.DayTime.DAY)
        pane.gameMode = runCatching { GameMode.valueOf(state.gameMode) }
            .getOrDefault(GameMode.CRYSTALS)

        // Humans and bots both take a slot. Bots are marked so the pane draws the bot portrait and
        // leaves them alone. The snapshot replaces the whole arrangement, so joining players appear
        // without the host touching anything.
        for (i in 0 until GameSetupPane.SLOT_COUNT) {
            val member = state.members.getOrNull(i)
            pane.setPlayerSlot(member?.toPlayer(), i)
            pane.slots.get(i).isAI = member?.isAi == true
            pane.slots.get(i).botDifficulty = member?.botDifficulty ?: "normal"
            pane.slots.get(i).isHostSlot = member?.host == true
        }

        // The host's own seat came back as a server copy, but the local mark has to stay on the
        // object the host can still move. A seat index of -1 would be a crash, and it happens if
        // the server ever seats the host somewhere the pane has not got.
        if (isHost) {
            val own = pane.slots.indexOfFirst { it.player?.clientId == myPlayer.clientId }
            if (own >= 0) pane.setPlayerSlot(myPlayer, own)
        }

        GameLog.info(
            "ServerLobbySetupStage",
            "lobby ${state.id}: ${pane.seatedCount} seated, map=${state.mapId}, " +
                    "fog=${state.fogOfWar}, host=$isHost"
        )
    }

    private fun LobbyMember.toPlayer(): Player? {
        if (name.isEmpty()) return null

        // Keep our own seat as the same object, so the local player mark survives, but take the
        // id the server gave us. The id is the colour, and seating is the server's to decide: a
        // joiner who kept their placeholder id would show themselves in the host's colour.
        if (clientId == GamePref.clientId) {
            myPlayer.id = playerId
            myPlayer.teamId = playerId
            return myPlayer
        }

        val player = Player(name = name, id = playerId, teamId = playerId)
        player.clientId = clientId
        // Keep the species the host picked. Rerolling it here made a seat visibly change species
        // every time the lobby state came back.
        player.species = species.ifBlank { Species.names.random() }
        return player
    }

    /**
     * Shows the map the host chose, from the map itself rather than by id, because a map may only
     * exist on the host's machine. A client that receives a lobby state applies the map the same
     * way, which is why this is one function: host and joiner run identical code.
     */
    private fun showChosenMap(mapId: String, hostMapJson: String?) {
        chosenMapId = mapId
        chosenMapJson = hostMapJson

        val loaded = if (hostMapJson != null) {
            runCatching { createMapFromJson(hostMapJson) }
        } else {
            runCatching { loadMap(mapId) }
        }

        loaded
            .onSuccess { pane.setMap(it) }
            .onFailure { addNotifyWindow("Map '$mapId' not found", "Lobby") }

        // Only a host's own choice is worth sending back. A map that arrived in a lobby state is
        // the server repeating what it already has.
        if (!applyingRemoteState) createLobby() else refresh()
    }

    /**
     * A map that only exists on this machine. The server cannot load it by id, so the map
     * itself travels with the lobby and is handed to every client in `match_found`.
     */
    private fun pickOwnMap(map: MapConverted) {
        chosenMapId = map.mapId.ifBlank { map.mapSave.id ?: "host map" }
        chosenMapJson = map.mapSave.toGson()
        pane.setMap(map)
        if (!applyingRemoteState) createLobby() else refresh()
    }

    /**
     * The host picks the map from their own machine, and the server only carries it. The server's
     * own pool is deliberately not offered: a server is not the author of a match, it is the place
     * the host's match is held while players join it.
     */
    private fun showMapPicker() {
        val picker = VisWindow("Choose a map")
        with(picker) {
            setCenterOnAdd(true)
            closeOnEscape()
            add().width(280f)
            row()

            add(newLabel("Pick one of your own maps.", "default")).padBottom(8f).row()
            add(newTextButton("Your maps ...").addChangeListener {
                picker.remove()
                openOwnMapBrowser()
            }).fillX().row()
            add(newTextButton(strings.general.cancel).addChangeListener { picker.remove() })
                .fillX().padTop(6f)

            pack()
        }
        addActor(picker)
    }

    /** Reuses the normal local map browser, the same one the LAN host uses. */
    private fun openOwnMapBrowser() {
        menuScreen.stage = menuScreen.mapSelectionStage.also { stage ->
            stage.onResult = { result ->
                result?.map?.let { pickOwnMap(it) }
                result?.gameMode?.let { pane.gameMode = it }

                // Coming back re-enters this screen, so the host keeps their seat and settings.
                menuScreen.stage = this
            }
        }
    }

    /**
     * Sends the whole arrangement to the server. Called when the lobby is first opened and again
     * after every change, so the server is never behind what the host is looking at.
     *
     * Returns false only when there is nothing to send yet, which is a lobby with no map. The map
     * itself always travels with the request, since it may exist only on the host's machine.
     */
    private fun createLobby(): Boolean {
        // Nothing to send until a map is chosen, which is what the status line reports.
        val mapId = chosenMapId ?: run { refresh(); return false }

        val request = CreateLobby()
        request.name = "${GamePref.displayName}'s game"
        request.mapId = mapId
        request.mapName = pane.map?.mapSave?.name ?: ""
        request.fogOfWar = pane.fogOfWar
        request.gameMode = pane.gameMode.name
        request.season = pane.season.name
        request.daytime = pane.daytime.name
        request.listed = listed
        chosenMapJson?.let { request.mapJson = it }

        // The arrangement on screen is the arrangement in game, with each seat keeping the
        // player id the lobby already gave it, because the id is the colour.
        pane.slots.forEachIndexed { index, slot ->
            val player = slot.player ?: return@forEachIndexed

            request.seats.add(LobbySeat().also {
                it.slot = index
                it.playerId = player.id
                it.name = player.name
                it.isAi = slot.isAI
                it.botDifficulty = slot.botDifficulty
                it.species = player.species
            })
        }

        // The room is the full set of seats, never the current headcount. Sizing it by the
        // headcount meant a host who sat past the map's places to watch filled the room
        // themselves and nobody could join.
        request.maxPlayers = GameSetupPane.SLOT_COUNT

        GameLog.info(
            "ServerLobbySetupStage",
            "host sends: " + SeatingLog.describe(
                "seats",
                request.seats.map {
                    SeatingLog.row(it.slot, it.playerId, it.name, it.isAi, it.botDifficulty)
                },
            ) + " map=$mapId hostMap=${chosenMapJson != null}" +
                    " fog=${request.fogOfWar} listed=${request.listed}"
        )

        client.createLobby(request)
        return true
    }

    private fun startMatch() {
        if (!isHost) {
            addNotifyWindow("Waiting for the host to start ...", "Lobby")
            return
        }
        client.startMatch()
    }

    /**
     * The lobby already exists; this only reflects where it has got to.
     *
     * Starting needs a second player or bot, so a lobby nobody has joined cannot be launched into
     * a one player match. Publishing is the host's own choice and can be undone.
     */
    private fun refresh() {
        if (!inLobby) {
            openLobbyButton.isDisabled = true
            startButton.isDisabled = true
            statusLabel.setText(
                if (chosenMapId == null) "Choose a map to begin."
                else "Setting up the lobby ..."
            )
            return
        }

        val waiting = pane.seatedCount < 2

        openLobbyButton.setText(if (listed) "Close" else "Open")
        startButton.setText("Start")

        // A joiner has nothing to press here. Publishing and starting are the host's, and a long
        // disabled button in a footer this narrow only crowds out the one that matters.
        openLobbyButton.isVisible = isHost
        startButton.isVisible = isHost

        openLobbyButton.isDisabled = chosenMapId == null
        startButton.isDisabled = waiting

        statusLabel.setText(
            when {
                !isHost -> "The host starts the match."
                !listed -> "Private lobby. Open it to be listed."
                waiting -> "Waiting for a player. ${pane.seatedCount} seated."
                else -> "${pane.seatedCount} seated. Ready to start."
            }
        )
    }

    private fun back() {
        if (inLobby) {
            client.leaveLobby()
            onLeftLobby()
        } else {
            leaving = true
            menuScreen.stage = menuScreen.lobbiesStage
        }
    }

    /**
     * Dropped into the browser when this screen hands the connection over, so leaving a lobby
     * returns to the list rather than the connect form.
     */
    fun onLeftLobby() {
        inLobby = false
        leaving = true
        menuScreen.stage = menuScreen.lobbiesStage
    }

    /** Network callbacks arrive on netty threads, so hop back to the render thread. */
    private fun onUi(block: () -> Unit) {
        Gdx.app.postRunnable(block)
    }

    override fun addActor(actor: Actor?) {
        if (actor is VisWindow) {
            actor.isMovable = false
            windowTable.clear()
            windowTable.add(actor).expand().center().maxWidth(viewport.worldWidth - 10f)
            return
        }

        super.addActor(actor)
    }
}
