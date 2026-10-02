package ctmn.petals.menu

import com.badlogic.gdx.Gdx
import com.badlogic.gdx.Input
import com.badlogic.gdx.scenes.scene2d.Actor
import com.badlogic.gdx.scenes.scene2d.InputEvent
import com.badlogic.gdx.scenes.scene2d.InputListener
import com.badlogic.gdx.scenes.scene2d.Stage
import com.badlogic.gdx.utils.Align
import com.kotcrab.vis.ui.widget.VisTable
import com.kotcrab.vis.ui.widget.VisTextField
import com.kotcrab.vis.ui.widget.VisWindow
import ctmn.petals.screens.MenuScreen
import ctmn.petals.actors.actions.TimeAction
import ctmn.petals.Config
import ctmn.petals.GamePref
import ctmn.petals.net.LanHost
import ctmn.petals.net.ServerClient
import ctmn.petals.net.proto.Welcome
import ctmn.petals.strings
import ctmn.petals.utils.closeJmDNS
import ctmn.petals.utils.startJmDNSAsClient
import ctmn.petals.utils.startJmDNSAsServer
import ctmn.petals.widgets.*

class MpLobbyVariantsStage(private val menuScreen: MenuScreen) : Stage(menuScreen.viewport, menuScreen.batch) {

    /**
     * Three ways to meet another player, and nothing else. Each one has its own screen behind it:
     * hot seat, the peer hosted LAN room, and the dedicated server browser. Keeping them one press
     * apart is what stops this screen from turning into a pile of unrelated controls.
     */
    private val sameScreenButton = newTextButton(strings.general.same_screen)
    private val lanButton = newTextButton("LAN")
    private val onlineButton = newTextButton("Online")

    // The LAN room: host, or join by address or by searching.
    private val labelLocalMp = newLabel(strings.general.local_multiplayer, "default")
    private val serverLobbyButton = newTextButton(strings.general.host)
    private val ipTextField = VisTextField()

    //private val clientLobbyButton = newTextButton("Connect")
    private val serverSearchButton = newImageButton("server_search")
    private val serverConnectButton = newImageButton("server_connect")

    private val returnButton = newTextButton(strings.general.return_)

    /** The LAN room sits behind the chooser, so it needs its own way back. */
    private val lanReturnButton = newTextButton(strings.general.return_)

    private val table = VisTable()
    private val lanTable = VisTable()
    private val windowTable = VisTable()

    init {
//        addListener {
//            if (it is ResetStateEvent) {
//                ipTextField.clearText()
//            }
//
//            false
//        }

        ipTextField.messageText = "localhost" //"Ip Address"
        ipTextField.setAlignment(Align.center)

        addListener(object : InputListener() {
            override fun touchDown(event: InputEvent?, x: Float, y: Float, pointer: Int, button: Int): Boolean {
                //keyboardFocus = null

                return super.touchDown(event, x, y, pointer, button)
            }

            override fun keyDown(event: InputEvent?, keycode: Int): Boolean {
                if (keycode == Input.Keys.ENTER)
                    keyboardFocus = null

                return super.keyDown(event, keycode)
            }
        })

        addActor(table)
        with(table) {
            setFillParent(true)
            center()

            add(newLabel(strings.general.vsPlayer, "default")).padBottom(12f)
            row()

            add(sameScreenButton).width(200f).padBottom(6f)
            row()
            add(lanButton).width(200f).padBottom(6f)
            row()
            add(onlineButton).width(200f)
            row()

            add(returnButton).padTop(24f)
        }

        // The LAN room is its own screen behind the LAN button. Hidden by default: two visible
        // fill-parent tables would draw on top of each other.
        lanTable.setFillParent(true)
        lanTable.center()
        lanTable.isVisible = false
        addActor(lanTable)

        with(lanTable) {
            add(labelLocalMp).padBottom(12f)
            row()
            add(serverLobbyButton).minWidth(160f).padBottom(16f)
            row()

            add(VisTable().apply {
                add(serverSearchButton)
                add(ipTextField).fillX().expandX()
                add(serverConnectButton)
            }).width(300f)
            row()

            add(lanReturnButton).padTop(24f)
        }

        windowTable.setFillParent(true)
        addActor(windowTable)

        sameScreenButton.addChangeListener {
            menuScreen.stage = menuScreen.botGameSetupStage
        }

        // Hosting a room starts a real server inside this game. Everything after that is the same
        // path the online flow takes: ServerLobbySetupStage, then OnlinePlayScreen.
        serverLobbyButton.addChangeListener {
            hostLan()
        }

        serverSearchButton.addChangeListener {
            searchLan()
        }

        serverConnectButton.addChangeListener {
            serverConnect()
        }

        // Online goes straight to the browser, which connects to the configured server on its own.
        // The address form is only reached from there, and only for a server somewhere else.
        onlineButton.addChangeListener {
            menuScreen.stage = menuScreen.lobbiesStage
        }

        lanButton.addChangeListener {
            table.isVisible = false
            lanTable.isVisible = true
        }

        lanReturnButton.addChangeListener {
            lanTable.isVisible = false
            table.isVisible = true
        }

        returnButton.addChangeListener {
            menuScreen.stage = menuScreen.menuStage
        }
    }

    /**
     * Opens a room on this machine. The room is a normal server, so the host joins it the same way
     * anybody else would; only the address is local.
     */
    private fun hostLan() {
        val started = runCatching { LanHost.start() }
        if (started.isFailure) {
            addActor(newNotifyWindow("LAN port ${Config.LAN_PORT} is busy.", "LAN"))
            return
        }

        // Advertise the room so a joiner can find it without typing an address.
        startJmDNSAsServer { ok ->
            if (!ok) Gdx.app.log(javaClass.simpleName, "The room is up, but discovery failed.")
        }

        val host = "localhost"
        val client = ServerClient()

        client.listener = object : ServerClient.Listener {
            override fun onWelcome(welcome: Welcome) {
                onUi {
                    menuScreen.stage = ServerLobbySetupStage(
                        menuScreen = menuScreen,
                        client = client,
                        serverHost = host,
                        serverPort = Config.LAN_PORT,
                        serverMaps = welcome.mapIds.toList(),
                        lobbyId = null,
                        // A LAN room is reachable only from the same network, so it starts open.
                        startOpen = true,
                    ).apply {
                        onLeave = {
                            LanHost.stop()
                            closeJmDNS()
                            menuScreen.stage = this@MpLobbyVariantsStage
                        }
                    }
                }
            }

            override fun onConnectFailed(cause: Throwable?) {
                onUi {
                    client.close()
                    LanHost.stop()
                    addActor(newNotifyWindow("Could not start the LAN room.", "LAN"))
                }
            }
        }

        client.connect(host, Config.LAN_PORT, GamePref.clientId, GamePref.displayName)
    }

    /** Looks for a room on the local network and opens the browser on it. */
    private fun searchLan() {
        serverSearchButton.isDisabled = true

        val loadingCover = LoadingCover(strings.general.connecting)
        addActor(loadingCover)
        loadingCover.setLabelText(strings.general.looking_for_server)

        var finished = false

        fun finish(host: String?) {
            if (finished) return
            finished = true

            serverSearchButton.clearActions()
            serverSearchButton.isDisabled = false
            closeJmDNS()
            loadingCover.done()
            loadingCover.remove()

            if (host == null)
                addActor(newNotifyWindow(strings.general.no_local_servers_found, strings.general.connect))
            else
                connectLan(host)
        }

        startJmDNSAsClient { host -> onUi { finish(host) } }

        // Give up after a few seconds, the same as before.
        serverSearchButton.addAction(TimeAction {
            if (it < 5f) return@TimeAction false

            onUi { finish(null) }
            true
        })
    }

    /** Connects to the address in the field and opens the browser on it. */
    private fun serverConnect() {
        connectLan(ipTextField.text.ifEmpty { "localhost" })
    }

    /**
     * Opens the lobby browser on a room on the network. The browser is the online one, pointed at
     * this address instead of the dedicated server, so there is nothing LAN specific behind it.
     */
    private fun connectLan(host: String) {
        val client = ServerClient()

        client.listener = object : ServerClient.Listener {
            override fun onWelcome(welcome: Welcome) {
                onUi {
                    menuScreen.lobbiesStage.onLeave = { menuScreen.stage = this@MpLobbyVariantsStage }
                    menuScreen.stage = menuScreen.lobbiesStage.connect(client, host, Config.LAN_PORT, welcome)
                }
            }

            override fun onConnectFailed(cause: Throwable?) {
                onUi {
                    client.close()
                    addActor(newNotifyWindow("No LAN room at $host.", "LAN"))
                }
            }
        }

        client.connect(host, Config.LAN_PORT, GamePref.clientId, GamePref.displayName)
    }

    /** Network and discovery callbacks arrive on other threads, so hop back to the render thread. */
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