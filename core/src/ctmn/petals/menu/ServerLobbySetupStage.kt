package ctmn.petals.menu

import com.badlogic.gdx.Gdx
import com.badlogic.gdx.scenes.scene2d.Actor
import com.badlogic.gdx.scenes.scene2d.Stage
import com.kotcrab.vis.ui.VisUI
import com.kotcrab.vis.ui.widget.VisImage
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
import ctmn.petals.net.proto.KickedMsg
import ctmn.petals.net.proto.LobbyClosedMsg
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
    /** Maps the server can host, offered below the host's own maps. */
    private val serverMaps: List<String>,
    /** Null when this screen creates a new lobby rather than opening an existing one. */
    private val lobbyId: String?,
    /**
     * Whether the room starts published. A dedicated server's lobby is private until the host
     * opens it; a LAN room starts open, since everybody who can reach it is already local.
     */
    private val startOpen: Boolean = false,
) : Stage(menuScreen.viewport, menuScreen.batch) {

    private val pane = GameSetupPane(this)

    private val roomStatusImage = VisImage(VisUI.getSkin().getDrawable("room_status_problem"))

    private val returnButton = newTextButton(strings.general.return_)

    /**
     * Publishes the lobby so it appears in the browser, or unpublishes it again. The lobby exists
     * from the moment this screen opens, so this is only about who can see it.
     */
    private val openLobbyButton = newTextButton("Open lobby").apply { isDisabled = true }

    /** Runs the match the lobby is waiting for. Host only, and only with somebody to play. */
    private val startButton = newTextButton(strings.general.confirm).apply { isDisabled = true }

    private val table = VisTable()
    private val windowTable = VisTable()

    /** Set once the server confirmed our lobby, so the buttons reflect a real seat. */
    private var inLobby = false

    /** Whether the lobby is currently in the browser. Mirrors what the server last told us. */
    private var listed = startOpen

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
    ).apply {
        clientId = GamePref.clientId
        species = GamePref.species
    }

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
            onUi {
                // A refused join (banned, full, already playing) leaves this screen with nothing
                // on it, so go back to the browser and say why there. An error after we are in
                // belongs to this screen, which is still showing the lobby.
                if (inLobby) addNotifyWindow(error.message, "Lobby")
                else leaveToBrowser(error.message)
            }
        }

        override fun onKicked(kicked: KickedMsg) {
            onUi {
                leaveToBrowser(kicked.reason.ifBlank { "You were removed from the lobby" })
            }
        }

        override fun onLobbyClosed(closed: LobbyClosedMsg) {
            onUi {
                leaveToBrowser(closed.reason.ifBlank { "The host closed the lobby" })
            }
        }

        override fun onDisconnected() {
            onUi {
                if (leaving || menuScreen.game.screen is OnlinePlayScreen) return@onUi
                leaveToBrowser("Lost connection to the server")
            }
        }
    }

    init {
        pane.allowLocalPlayer = false
        // Every change goes straight back to the server, so the players already sitting in the
        // lobby see the same arrangement the host is looking at. A lobby with no map yet sends
        // one with no map: it exists from the start, map or not. A joiner's own move travels as
        // a seat request instead, since the arrangement is not theirs to author.
        pane.onLobbyChanged = {
            if (!applyingRemoteState && isHost) createLobby()
        }
        pane.onLocalSeatMoved = { slot ->
            if (!applyingRemoteState && !isHost) client.moveSeat(slot)
        }
        // A player may change their own species. The host changes anybody's through the whole
        // arrangement instead, which is the same thing it always was.
        pane.onLocalSpeciesChanged = { species ->
            if (!applyingRemoteState && !isHost) client.setSpecies(species)
        }
        // Only the host is offered these in the pane, and the server checks again.
        pane.onPlayerKicked = { player, banned ->
            player.clientId?.let { client.kick(it, banned) }
        }
        pane.onUnplayableMap = { message -> addNotifyWindow(message, "Lobby") }

        pane.changeMapButton.addChangeListener { showMapPicker() }

        returnButton.addChangeListener { back() }

        // Publishing is the host's own switch, and lives above the settings rather than in the
        // footer, so the footer is the same back, status, confirm row as the LAN room's.
        with(pane.aboveSettings) {
            add(openLobbyButton).width(300f)
        }
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
                add(roomStatusImage).size(42f).expandX().center()
                add(startButton).right()
            }).width(300f)
        }

        table.setFillParent(true)
        table.center()

        windowTable.setFillParent(true)
        addActor(windowTable)

        client.listener = listener

        refresh()

        // Our own seat object, set for a joiner as much as for the host: the local player mark
        // follows it, and a seat move has to know which seat is ours. Without it a joiner could
        // take a free seat but never swap with one that was taken.
        pane.localPlayer = myPlayer

        if (lobbyId != null) {
            // Our own species travels with the join, otherwise the host's lobby has nothing to
            // show for us and every update rerolls it.
            client.joinLobby(lobbyId, myPlayer.species)
        } else {
            // Creating: sit in the first slot straight away, as the local flow does, and make the
            // lobby straight away too. It stays private until the host publishes it, so the
            // arrangement can be built in peace and appears in the browser when it is ready.
            pane.hostPlayer = myPlayer
            pane.setPlayerSlot(myPlayer, 0)
            createLobby()
        }
    }

    /** Writes the server's view of the lobby into the pane. Never reports a local change. */
    private fun applyLobbyState(state: LobbyStateMsg) {
        inLobby = true
        isHost = state.hostClientId == GamePref.clientId
        pane.isHost = isHost

        roomStatusImage.drawable = VisUI.getSkin().getDrawable("room_status_ok")

        if (state.mapId.isNotBlank() && state.mapId != chosenMapId) {
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
        var hostSeat: Player? = null
        for (i in 0 until GameSetupPane.SLOT_COUNT) {
            val member = state.members.getOrNull(i)
            pane.setPlayerSlot(member?.toPlayer(), i)
            pane.slots.get(i).isAI = member?.isAi == true
            pane.slots.get(i).botDifficulty = member?.botDifficulty ?: "normal"
            if (member?.host == true) hostSeat = pane.slots.get(i).player
        }

        // The host's own seat came back as a server copy, but the local mark has to stay on the
        // object the host can still move. A seat index of -1 would be a crash, and it happens if
        // the server ever seats the host somewhere the pane has not got.
        if (isHost) {
            val own = pane.slots.indexOfFirst { it.player?.clientId == myPlayer.clientId }
            if (own >= 0) pane.setPlayerSlot(myPlayer, own)
        }

        // The crown follows the host, not the slot, so remember who they are.
        pane.hostPlayer = hostSeat ?: myPlayer.takeIf { isHost }

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

            // The host can change anybody's species, ours included, so take theirs.
            species.takeIf { it.isNotBlank() }?.let { myPlayer.species = it }

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

    /** Pick a map the server itself has, by id. */
    private fun pickServerMap(mapId: String) {
        chosenMapId = mapId
        chosenMapJson = null

        runCatching { loadMap(mapId) }
            .onSuccess { pane.setMap(it) }
            .onFailure { addNotifyWindow("Map '$mapId' not found", "Lobby") }

        if (!applyingRemoteState) createLobby() else refresh()
    }

    /**
     * The host's own maps come first, since hosting your own is the common case. The server's maps
     * are listed under them for a host who would rather use one that every client already has.
     */
    private fun showMapPicker() {
        val picker = VisWindow("Choose a map")
        with(picker) {
            setCenterOnAdd(true)
            closeOnEscape()
            add().width(280f)
            row()

            add(newTextButton("Your maps ...").addChangeListener {
                picker.remove()
                openOwnMapBrowser()
            }).fillX().row()

            if (serverMaps.isEmpty()) {
                add(newTextButton(strings.general.cancel).addChangeListener { picker.remove() })
                    .fillX().padTop(6f)
            } else {
                add(newLabel("Server maps", "default")).padTop(8f).padBottom(4f).row()
                serverMaps.forEach { mapId ->
                    add(newTextButton(mapId).addChangeListener {
                        pickServerMap(mapId)
                        picker.remove()
                    }).fillX().row()
                }
                add(newTextButton(strings.general.cancel).addChangeListener { picker.remove() })
                    .fillX().padTop(6f)
            }

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
     * Sends the whole arrangement to the server. Called when this screen opens and again after
     * every change, so the server is never behind what the host is looking at.
     *
     * A map is not required. The lobby exists from the moment the screen opens so the host can
     * publish it, fill it and pick a map in any order; only starting a match needs a map. The map
     * itself travels with the request when it came from the host's machine.
     */
    private fun createLobby(): Boolean {
        val request = CreateLobby()
        request.name = "${GamePref.displayName}'s game"
        request.mapId = chosenMapId ?: ""
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
                // Which connection this seat is, so the server can tell the host's own seat from
                // a joiner's and keep the host's arrangement of people.
                it.clientId = player.clientId ?: ""
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
            ) + " map=${request.mapId} hostMap=${chosenMapJson != null}" +
                    " fog=${request.fogOfWar} listed=${request.listed}"
        )

        client.createLobby(request)
        refresh()
        return true
    }

    private fun startMatch() {
        if (!isHost) {
            addNotifyWindow("Waiting for the host to start ...", "Lobby")
            return
        }
        if (pane.map == null) {
            addNotifyWindow("Choose a map first.", "Lobby")
            return
        }
        if (pane.seatedCount < 2) {
            addNotifyWindow("Somebody has to play against you.", "Lobby")
            return
        }
        if (!pane.allPlacesFilled) {
            addNotifyWindow("Every place on the map needs somebody.", "Lobby")
            return
        }
        client.startMatch()
    }

    /**
     * The lobby already exists; this only reflects where it has got to. Starting needs a map with
     * every place filled and somebody to play against, so a half arranged lobby cannot launch.
     */
    private fun refresh() {
        openLobbyButton.setText(if (listed) "Close lobby" else "Open lobby")

        // A joiner has nothing to press here: publishing and starting are the host's.
        openLobbyButton.isVisible = isHost
        startButton.isVisible = isHost

        // Publishing does not wait for a map. The lobby is a room; the map is what fills it.
        openLobbyButton.isDisabled = !isHost
        startButton.isDisabled = !pane.canStart

        if (inLobby) {
            GameLog.verbose(
                "ServerLobbySetupStage",
                "lobby refresh: seated=${pane.seatedCount} places=${pane.placeIndices} " +
                        "filled=${pane.allPlacesFilled} listed=$listed"
            )
        }
    }

    private fun back() {
        // Always sent, not only once a state has arrived: the lobby exists from the moment this
        // screen opened, and a leave with no lobby on the server is simply ignored there.
        client.leaveLobby()
        onLeftLobby()
    }

    /**
     * Where the screen goes when the lobby is left. Defaults to the online lobby browser; a LAN
     * host sets it to its own screen so the room it runs can be stopped on the way out.
     */
    var onLeave: (() -> Unit)? = null

    /**
     * Dropped into the browser when this screen hands the connection over, so leaving a lobby
     * returns to the list rather than the connect form.
     */
    fun onLeftLobby() {
        inLobby = false
        leaving = true

        val leaveTo = onLeave
        if (leaveTo != null) leaveTo() else menuScreen.stage = menuScreen.lobbiesStage
    }

    /**
     * Returns to the browser, saying why on the screen we land on.
     *
     * The message has to be added after the switch: a notify added here would go with the stage we
     * are leaving, so the player would land on an empty lobby with nothing to explain it.
     */
    private fun leaveToBrowser(message: String) {
        onLeftLobby()
        menuScreen.stage.addNotifyWindow(message, "Lobby")
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
