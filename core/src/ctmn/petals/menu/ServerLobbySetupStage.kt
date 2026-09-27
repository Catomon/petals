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
import ctmn.petals.map.loadMap
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
    private val mapPool: List<String>,
    /** Null when this screen creates a new lobby rather than opening an existing one. */
    private val lobbyId: String?,
) : Stage(menuScreen.viewport, menuScreen.batch) {

    private val pane = GameSetupPane(this)

    private val roomStatusImage = VisImage(VisUI.getSkin().getDrawable("room_status_problem"))

    private val returnButton = newTextButton(strings.general.return_)
    private val confirmButton = newTextButton(strings.general.confirm).apply { isDisabled = true }

    private val table = VisTable()
    private val windowTable = VisTable()

    /** Set once the server confirmed our lobby, so the buttons reflect a real seat. */
    private var inLobby = false

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

                inLobby = true
                isHost = state.hostClientId == GamePref.clientId
                pane.isHost = isHost
                roomStatusImage.drawable = VisUI.getSkin().getDrawable("room_status_ok")

                if (state.mapId != null && state.mapId != chosenMapId) pickMap(state.mapId)
                pane.fogOfWar = state.fogOfWar

                // Humans and bots both take a slot. Bots are marked so the pane draws the bot
                // portrait and leaves them alone.
                for (i in 0 until GameSetupPane.SLOT_COUNT) {
                    val member = state.members.getOrNull(i)
                    pane.setPlayerSlot(member?.toPlayer(), i)
                    pane.slots.get(i).isAI = member?.isAi == true
                    pane.slots.get(i).botDifficulty = member?.botDifficulty ?: "normal"
                    pane.slots.get(i).isHostSlot = member?.host == true
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
        pane.onLobbyChanged = { refresh() }
        pane.onUnplayableMap = { message -> addNotifyWindow(message, "Lobby") }

        pane.changeMapButton.addChangeListener { showMapPicker() }

        returnButton.addChangeListener { back() }
        // One button, like the local setup screen: creating a lobby and starting it are the
        // same press. The host is the only one who may start, and the host is always seated.
        confirmButton.addChangeListener {
            if (inLobby) startMatch() else createLobbyAndStart()
        }

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
                add(confirmButton).right()
            }).width(300f)
        }

        table.setFillParent(true)
        table.center()

        windowTable.setFillParent(true)
        addActor(windowTable)

        client.listener = listener

        refresh()

        if (lobbyId != null) {
            client.joinLobby(lobbyId)
        } else {
            // Creating: sit in the first slot straight away, as the local flow does, and offer
            // the server's first map so there is something to look at.
            pane.localPlayer = myPlayer
            pane.setPlayerSlot(myPlayer, 0)
            mapPool.firstOrNull()?.let { pickMap(it) }
            refresh()
        }
    }

    private fun LobbyMember.toPlayer(): Player? {
        if (name.isEmpty()) return null

        // Keep our own seat as the same object, so the local player mark survives.
        if (clientId == GamePref.clientId) return myPlayer

        val player = Player(name = name, id = playerId, teamId = playerId)
        player.clientId = clientId
        // Keep the species the host picked. Rerolling it here made a seat visibly change species
        // every time the lobby state came back.
        player.species = species.ifBlank { Species.names.random() }
        return player
    }

    private fun pickMap(mapId: String) {
        chosenMapId = mapId
        chosenMapJson = null
        runCatching { loadMap(mapId) }
            .onSuccess { pane.setMap(it) }
            .onFailure { addNotifyWindow("Map '$mapId' not found", "Lobby") }
        refresh()
    }

    /**
     * A map that only exists on this machine. The server cannot load it by id, so the map
     * itself travels with the lobby and is handed to every client in `match_found`.
     */
    private fun pickOwnMap(map: MapConverted) {
        chosenMapId = map.mapId.ifBlank { map.mapSave.id ?: "host map" }
        chosenMapJson = map.mapSave.toGson()
        pane.setMap(map)
        refresh()
    }

    private fun showMapPicker() {
        val picker = VisWindow("Choose a map")
        with(picker) {
            setCenterOnAdd(true)
            closeOnEscape()
            add().width(280f)
            row()

            // The host's own maps first, since hosting your own map is the common case.
            add(newTextButton("Your maps ...").addChangeListener {
                picker.remove()
                openOwnMapBrowser()
            }).fillX().row()

            if (mapPool.isEmpty()) {
                add(newTextButton(strings.general.cancel).addChangeListener { picker.remove() }).fillX()
            } else {
                add(newLabel("Server maps", "default")).padTop(6f).row()
                for (mapId in mapPool) {
                    add(newTextButton(mapId).addChangeListener {
                        pickMap(mapId)
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
     * Creates the lobby and starts it in one go. Both messages are handled on the server's
     * single session thread in order, so the lobby exists by the time the start arrives.
     */
    private fun createLobbyAndStart() {
        if (!createLobby()) return
        client.startMatch()
    }

    private fun createLobby(): Boolean {
        val mapId = chosenMapId ?: return false

        val request = CreateLobby()
        request.name = "${GamePref.displayName}'s game"
        request.mapId = mapId
        request.fogOfWar = pane.fogOfWar
        request.gameMode = pane.gameMode.name
        chosenMapJson?.let { request.mapJson = it }

        // The arrangement on screen is the arrangement in game, with each seat keeping the
        // player id the lobby already gave it, because the id is the colour.
        var seated = 0
        pane.slots.forEachIndexed { index, slot ->
            val player = slot.player ?: return@forEachIndexed
            seated++

            request.seats.add(LobbySeat().also {
                it.slot = index
                it.playerId = player.id
                it.name = player.name
                it.isAi = slot.isAI
                it.botDifficulty = slot.botDifficulty
                it.species = player.species
            })
        }
        request.maxPlayers = seated.coerceIn(2, 8)

        GameLog.info(
            "ServerLobbySetupStage",
            "host sends: " + SeatingLog.describe(
                "seats",
                request.seats.map {
                    SeatingLog.row(it.slot, it.playerId, it.name, it.isAi, it.botDifficulty)
                },
            ) + " map=$mapId hostMap=${chosenMapJson != null}"
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

    private fun refresh() {
        if (!inLobby) {
            confirmButton.setText(strings.general.confirm)
            confirmButton.isDisabled = chosenMapId == null || !isHost
            return
        }

        confirmButton.setText(strings.general.confirm)
        confirmButton.isDisabled = !isHost || !pane.canStart
    }

    private fun back() {
        if (inLobby) client.leaveLobby()
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
