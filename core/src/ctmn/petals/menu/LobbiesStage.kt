package ctmn.petals.menu

import com.badlogic.gdx.Gdx
import com.badlogic.gdx.scenes.scene2d.Actor
import com.badlogic.gdx.scenes.scene2d.Stage
import com.badlogic.gdx.utils.Align
import com.kotcrab.vis.ui.widget.VisScrollPane
import com.kotcrab.vis.ui.widget.VisTable
import com.kotcrab.vis.ui.widget.VisTextField
import com.kotcrab.vis.ui.widget.VisWindow
import ctmn.petals.Config
import ctmn.petals.GamePref
import ctmn.petals.multiplayer.OnlinePlayScreen
import ctmn.petals.net.ServerClient
import ctmn.petals.net.proto.*
import ctmn.petals.screens.MenuScreen
import ctmn.petals.strings
import ctmn.petals.utils.logMsg
import ctmn.petals.widgets.*

/**
 * Online lobby browser for the dedicated server: connect to a server by address, browse its
 * lobbies, create one, or jump into the quick match queue. Found matches are handed to
 * [OnlinePlayScreen].
 */
class LobbiesStage(private val menuScreen: MenuScreen) : Stage(menuScreen.viewport, menuScreen.batch) {

    private var client: ServerClient? = null
    private var serverName: String = ""
    private var connected = false
    private var queueing = false
    private var serverHost = "localhost"
    private var serverPort = Config.SERVER_PORT

    private val nameField = VisTextField().apply { messageText = "Your name" }
    private val addressField = VisTextField().apply { messageText = "localhost" }
    private val portField = VisTextField(Config.SERVER_PORT.toString())

    private val statusLabel = newLabel("Not connected", "default")
    private val queueLabel = newLabel("", "default").apply { isVisible = false }

    private val lobbyTable = VisTable()
    private val lobbyScroll = VisScrollPane(lobbyTable)

    private val connectButton = newTextButton("Connect")
    private val refreshButton = newTextButton("Refresh").apply { isDisabled = true }
    private val createButton = newTextButton("Create lobby").apply { isDisabled = true }
    private val quickButton = newTextButton("Quick match").apply { isDisabled = true }
    private val cancelQueueButton = newTextButton("Cancel search").apply { isVisible = false }
    private val returnButton = newTextButton(strings.general.return_)

    private val table = VisTable()
    private val windowTable = VisTable()

    init {
        nameField.text = GamePref.displayName

        connectButton.addChangeListener { connect() }
        refreshButton.addChangeListener { client?.listLobbies() }
        createButton.addChangeListener { createLobby() }
        quickButton.addChangeListener { queueJoin() }
        cancelQueueButton.addChangeListener {
            queueing = false
            cancelQueueButton.isVisible = false
            queueLabel.isVisible = false
            client?.queueLeave()
        }
        returnButton.addChangeListener { leave() }

        table.setFillParent(true)
        table.center()

        with(table) {
            add(newLabel("Online lobbies", "default")).padBottom(12f)
            row()

            add(newLabel("Name", "default")).padRight(6f)
            add(nameField).width(220f).row()

            add(newLabel("Server", "default")).padRight(6f)
            add(VisTable().apply {
                add(addressField).width(140f)
                add(portField).width(70f).padLeft(4f)
                add(connectButton).padLeft(4f)
            }).row()

            add(statusLabel).padTop(6f).row()
            add(queueLabel).padTop(4f).row()

            add(lobbyScroll).width(320f).height(200f).padTop(8f).row()

            add(VisTable().apply {
                add(refreshButton).width(150f).padRight(6f)
                add(createButton).width(160f)
            }).padTop(8f).row()

            add(VisTable().apply {
                add(quickButton).width(150f).padRight(6f)
                add(cancelQueueButton).width(160f)
            }).padTop(6f).row()

            // The old local / LAN host flow stays reachable from here.
            add(
                newTextButton("Local and LAN games").addChangeListener {
                    client?.close()
                    client = null
                    connected = false
                    menuScreen.stage = menuScreen.mpLobbyVariantsStage
                }
            ).width(320f).padTop(16f).row()

            add(returnButton).width(150f).padTop(10f)
        }

        addActor(table)

        windowTable.setFillParent(true)
        addActor(windowTable)

        showLobbies(emptyList())
    }

    private fun connect() {
        if (connected) {
            leave()
            return
        }

        val host = addressField.text.ifBlank { "localhost" }
        val port = portField.text.toIntOrNull() ?: Config.SERVER_PORT

        serverHost = host
        serverPort = port

        GamePref.displayName = nameField.text
        GamePref.save()

        statusLabel.setText("Connecting to $host:$port ...")

        val newClient = ServerClient()
        newClient.listener = object : ServerClient.Listener {

            override fun onWelcome(welcome: Welcome) {
                onUi {
                    connected = true
                    serverName = welcome.serverName
                    statusLabel.setText("Connected to ${welcome.serverName}")
                    connectButton.setText("Disconnect")
                    refreshButton.isDisabled = false
                    createButton.isDisabled = false
                    quickButton.isDisabled = false
                    newClient.listLobbies()
                }
            }

            override fun onLobbyList(list: LobbyList) {
                onUi { showLobbies(list.lobbies) }
            }

            override fun onLobbyState(state: LobbyStateMsg) {
                onUi { showLobbyState(state) }
            }

            override fun onQueueStatus(status: QueueStatus) {
                onUi {
                    queueing = true
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
                    queueing = false
                    queueLabel.isVisible = false
                    cancelQueueButton.isVisible = false

                    if (connected) {
                        client = null
                        connected = false
                    }
                    newClient.close()

                    menuScreen.game.screen = OnlinePlayScreen(newClient, found, serverHost, serverPort)
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

        try {
            newClient.connect(host, port, GamePref.clientId, GamePref.displayName)
            client = newClient
        } catch (e: Exception) {
            statusLabel.setText("Could not connect: ${e.message}")
            newClient.close()
        }
    }

    private fun createLobby() {
        val request = CreateLobby()
        request.name = "${GamePref.displayName}'s game"
        request.maxPlayers = 2
        request.fogOfWar = true
        request.mapId = ""
        client?.createLobby(request)
    }

    private fun queueJoin() {
        queueing = true
        queueLabel.isVisible = true
        cancelQueueButton.isVisible = true
        queueLabel.setText("Searching for a match ...")
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
        connectButton.setText("Connect")
        refreshButton.isDisabled = true
        createButton.isDisabled = true
        quickButton.isDisabled = true
        queueLabel.isVisible = false
        cancelQueueButton.isVisible = false
        showLobbies(emptyList())
    }

    private fun showLobbies(lobbies: List<LobbyInfo>) {
        lobbyTable.clear()

        if (lobbies.isEmpty()) {
            lobbyTable.add(newLabel("No lobbies", "default")).pad(6f)
            return
        }

        for (lobby in lobbies) {
            lobbyTable.add(
                newLabel("${lobby.name}  [${lobby.players}/${lobby.maxPlayers}]", "default")
            ).width(200f).left().padRight(6f)

            lobbyTable.add(newLabel(lobby.mapId, "default")).width(80f).left()

            lobbyTable.add(
                newTextButton("Join").addChangeListener {
                    client?.joinLobby(lobby.id)
                }
            ).padLeft(6f)

            lobbyTable.row().padTop(4f)
        }
    }

    private fun showLobbyState(state: LobbyStateMsg) {
        if (state.members.none { it.clientId == GamePref.clientId }) return

        addNotifyWindow(
            "Lobby '${state.name}' on ${state.mapId}\n" +
                state.members.joinToString("\n") { "${it.name} (ready: ${it.ready})" } +
                "\n\nWaiting for the match to start ...",
            "Lobby"
        )
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
