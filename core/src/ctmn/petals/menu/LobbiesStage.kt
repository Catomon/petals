package ctmn.petals.menu

import com.badlogic.gdx.Gdx
import com.badlogic.gdx.scenes.scene2d.Actor
import com.badlogic.gdx.scenes.scene2d.Stage
import com.kotcrab.vis.ui.widget.VisTable
import com.kotcrab.vis.ui.widget.VisWindow
import ctmn.petals.Config
import ctmn.petals.GamePref
import ctmn.petals.multiplayer.OnlinePlayScreen
import ctmn.petals.net.ServerClient
import ctmn.petals.net.proto.*
import ctmn.petals.screens.MenuScreen
import ctmn.petals.strings
import ctmn.petals.widgets.*

/**
 * The lobby browser for the dedicated server: the lobbies the server has, a way to open one, and
 * the quick match queue.
 *
 * It arrives here already connected, through [connect]. The address form lives in
 * [ServerConnectStage], so this screen is only ever about lobbies. Matches found through the
 * queue, and matches the host starts, are handed to [OnlinePlayScreen].
 */
class LobbiesStage(private val menuScreen: MenuScreen) :
    Stage(menuScreen.viewport, menuScreen.batch), MenuStageAware {

    private var client: ServerClient? = null
    private var serverName: String = ""
    private var serverHost: String = "localhost"
    private var serverPort: Int = 0
    private var connected = false

    /**
     * Kept so it can be re-attached after [ServerLobbySetupStage] borrows the client. Without
     * this the browser goes deaf the first time you come back from a lobby.
     */
    private var browserListener: ServerClient.Listener? = null

    private val statusLabel = newLabel("Not connected", "default")
    private val queueLabel = newLabel("", "default").apply { isVisible = false }

    /**
     * The lobby list, in the same list_item style as the map browser, so opening a lobby is one
     * press on the lobby itself instead of a row of labels with a button stuck on the end.
     */
    private val lobbyList = ButtonsScrollPane().apply {
        setScrollingDisabled(true, false)
        setFadeScrollBars(false)
        onButtonClick = { button ->
            (button.userObject as? LobbyInfo)?.let { openLobby(it) }
        }
    }

    private val connectButton = newTextButton("Connect to a server").apply { isVisible = false }
    private val refreshButton = newTextButton("Refresh").apply { isDisabled = true }
    private val createButton = newTextButton("Create lobby").apply { isDisabled = true }
    private val quickButton = newTextButton("Quick match").apply { isDisabled = true }
    private val cancelQueueButton = newTextButton("Cancel search").apply { isVisible = false }
    private val returnButton = newTextButton(strings.general.return_)

    private val table = VisTable()
    private val windowTable = VisTable()

    /**
     * Takes over an already connected client from [ServerConnectStage] and returns this stage,
     * so the caller can assign it in one line.
     */
    fun connect(
        client: ServerClient,
        host: String,
        port: Int,
        welcome: Welcome,
    ): LobbiesStage {
        this.client = client
        this.serverHost = host
        this.serverPort = port
        this.serverName = welcome.serverName
        this.connected = true

        statusLabel.setText("Connected to ${welcome.serverName}")
        connectButton.isVisible = false
        refreshButton.isDisabled = false
        createButton.isDisabled = false
        quickButton.isDisabled = false

        client.listener = listener()
        browserListener = client.listener
        client.listLobbies()

        return this
    }

    /**
     * Connects to the configured server straight away, which is the usual case: a server on this
     * machine. The address form is only needed for somewhere else, and a failure says so rather
     * than blocking the way to the menu.
     */
    fun connectToConfiguredServer() {
        statusLabel.setText("Connecting to ${Config.DEFAULT_SERVER_HOST} ... ")
        connectButton.isVisible = false

        val fresh = ServerClient()
        fresh.listener = object : ServerClient.Listener {
            override fun onWelcome(welcome: Welcome) {
                onUi { connect(fresh, Config.DEFAULT_SERVER_HOST, Config.SERVER_PORT, welcome) }
            }

            override fun onConnectFailed(cause: Throwable?) {
                onUi {
                    fresh.close()
                    connectButton.isVisible = true
                    statusLabel.setText(
                        "No server on ${Config.DEFAULT_SERVER_HOST}:${Config.SERVER_PORT}"
                    )
                }
            }

            override fun onDisconnected() {
                onUi { if (!connected) onServerGone() }
            }
        }

        client = fresh
        runCatching { fresh.connect(Config.DEFAULT_SERVER_HOST, Config.SERVER_PORT, GamePref.clientId, GamePref.displayName) }
            .onFailure {
                fresh.close()
                statusLabel.setText("Could not connect: ${it.message}")
            }
    }

    init {
        connectButton.addChangeListener { menuScreen.stage = menuScreen.serverConnectStage }
        refreshButton.addChangeListener { client?.listLobbies() }
        createButton.addChangeListener { createLobby() }
        quickButton.addChangeListener { queueJoin() }
        cancelQueueButton.addChangeListener {
            cancelQueueButton.isVisible = false
            queueLabel.isVisible = false
            createButton.isDisabled = false
            quickButton.isDisabled = false
            client?.queueLeave()
        }
        returnButton.addChangeListener { leave() }

        table.setFillParent(true)
        table.center()

        with(table) {
            add(newLabel("Lobbies", "default")).padBottom(10f).row()
            add(statusLabel).padBottom(6f).row()

            add(lobbyList).width(320f).height(240f).row()

            add(queueLabel).padTop(6f).row()

            add(VisTable().apply {
                add(createButton).width(150f).padRight(6f)
                add(quickButton).width(150f)
            }).padTop(10f).row()

            add(VisTable().apply {
                add(refreshButton).width(150f).padRight(6f)
                add(cancelQueueButton).width(150f)
            }).padTop(6f).row()

            add(connectButton).width(320f).padTop(10f).row()

            add(returnButton).width(150f).padTop(16f)
        }

        addActor(table)

        windowTable.setFillParent(true)
        addActor(windowTable)

        showLobbies(emptyList())
    }

    private fun listener() = object : ServerClient.Listener {
        override fun onLobbyList(list: LobbyList) {
            onUi { showLobbies(list.lobbies) }
        }

        override fun onLobbyState(state: LobbyStateMsg) {
            onUi { client?.listLobbies() }
        }

        override fun onQueueStatus(status: QueueStatus) {
            onUi {
                queueLabel.isVisible = true
                cancelQueueButton.isVisible = true
                queueLabel.setText(
                    if (status.position == 0) "Searching for a match ..."
                    else "Searching ... position ${status.position} of ${status.size}"
                )
            }
        }

        override fun onMatchFound(found: MatchFound) {
            onUi {
                queueLabel.isVisible = false
                cancelQueueButton.isVisible = false

                val activeClient = client
                if (activeClient == null) return@onUi

                // The browser hands the live connection to the match screen, which keeps it for
                // reconnects, so this screen must let go of it.
                client = null
                connected = false

                menuScreen.game.screen = OnlinePlayScreen(activeClient, found, serverHost, serverPort)
            }
        }

        override fun onError(error: ErrorMsg) {
            onUi { addNotifyWindow(error.message, serverName.ifBlank { "Server" }) }
        }

        override fun onConnectFailed(cause: Throwable?) {
            onUi { statusLabel.setText("Could not connect: ${cause?.message ?: "unknown error"}") }
        }

        override fun onDisconnected() {
            onUi {
                if (menuScreen.game.screen is OnlinePlayScreen) return@onUi
                onServerGone()
            }
        }
    }

    override fun onMenuShown() {
        // Connected on first entry, not when the menu screen is built: nothing here should touch
        // the network before the player asks for it.
        if (!connected) {
            connectToConfiguredServer()
            return
        }

        // The setup screen borrows the client while it is open, so take the listener back and
        // catch up on anything that happened while we were away.
        val activeClient = client
        if (activeClient != null && browserListener != null) {
            activeClient.listener = browserListener
            activeClient.listLobbies()
            refreshButton.isDisabled = false
            createButton.isDisabled = false
            quickButton.isDisabled = false
        }
    }

    private fun createLobby() {
        val activeClient = client ?: return
        menuScreen.stage = ServerLobbySetupStage(
            menuScreen = menuScreen,
            client = activeClient,
            serverHost = serverHost,
            serverPort = serverPort,
            lobbyId = null,
        )
    }

    private fun openLobby(lobby: LobbyInfo) {
        val activeClient = client ?: return
        menuScreen.stage = ServerLobbySetupStage(
            menuScreen = menuScreen,
            client = activeClient,
            serverHost = serverHost,
            serverPort = serverPort,
            lobbyId = lobby.id,
        )
    }

    private fun queueJoin() {
        queueLabel.isVisible = true
        cancelQueueButton.isVisible = true
        queueLabel.setText("Searching for a match ...")

        // Nothing else in the browser means anything while the queue is searching.
        createButton.isDisabled = true
        quickButton.isDisabled = true

        client?.queueJoin()
    }

    private fun leave() {
        client?.close()
        client = null
        connected = false
        menuScreen.stage = menuScreen.menuStage
    }

    private fun onServerGone() {
        if (!connected) return
        connected = false
        client = null
        statusLabel.setText("Disconnected")
        connectButton.isVisible = true
        refreshButton.isDisabled = true
        createButton.isDisabled = true
        quickButton.isDisabled = true
        queueLabel.isVisible = false
        cancelQueueButton.isVisible = false
        showLobbies(emptyList())
    }

    private fun showLobbies(lobbies: List<LobbyInfo>) {
        lobbyList.removeItems()

        if (lobbies.isEmpty()) {
            lobbyList.buttonsGroup.add(newLabel("No open lobbies", "default")).pad(10f)
            return
        }

        for (lobby in lobbies) {
            // Two lines in one button, so the map is named and the whole entry stays clickable.
            lobbyList.addButton(
                "${lobby.name}   ${lobby.players}/${lobby.maxPlayers}\n" +
                        lobby.mapName.ifBlank { lobby.mapId },
                lobby,
            )
        }
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
