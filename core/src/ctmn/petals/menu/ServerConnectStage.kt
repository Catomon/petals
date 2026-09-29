package ctmn.petals.menu

import com.badlogic.gdx.Gdx
import com.badlogic.gdx.scenes.scene2d.Actor
import com.badlogic.gdx.scenes.scene2d.Stage
import com.kotcrab.vis.ui.widget.VisTable
import com.kotcrab.vis.ui.widget.VisTextField
import com.kotcrab.vis.ui.widget.VisWindow
import ctmn.petals.Config
import ctmn.petals.GamePref
import ctmn.petals.screens.MenuScreen
import ctmn.petals.strings
import ctmn.petals.widgets.addChangeListener
import ctmn.petals.widgets.addNotifyWindow
import ctmn.petals.widgets.newLabel
import ctmn.petals.widgets.newTextButton

/**
 * Asks which dedicated server to connect to, and nothing else.
 *
 * The lobby browser used to carry the address fields as well, which made one screen do two jobs
 * and left the lobby list buried under connection settings. This screen collects the details,
 * hands the connected client to [LobbiesStage] and gets out of the way.
 */
class ServerConnectStage(private val menuScreen: MenuScreen) :
    Stage(menuScreen.viewport, menuScreen.batch) {

    private val nameField = VisTextField().apply { messageText = "Your name" }
    private val addressField = VisTextField().apply { messageText = Config.DEFAULT_SERVER_HOST }
    private val portField = VisTextField(Config.SERVER_PORT.toString())

    private val statusLabel = newLabel("", "default")

    private val connectButton = newTextButton(strings.general.connect)
    private val returnButton = newTextButton(strings.general.return_)

    private val table = VisTable()
    private val windowTable = VisTable()

    init {
        nameField.text = GamePref.displayName

        connectButton.addChangeListener { connect() }
        // Only reachable from the browser, so back goes there rather than all the way to the menu.
        returnButton.addChangeListener { menuScreen.stage = menuScreen.lobbiesStage }

        table.setFillParent(true)
        table.center()

        with(table) {
            add(newLabel("Dedicated server", "default")).padBottom(12f)
            row()

            add(newLabel("Name", "default")).padRight(6f)
            add(nameField).width(220f).row()

            add(newLabel("Address", "default")).padRight(6f)
            add(VisTable().apply {
                add(addressField).width(140f)
                add(portField).width(70f).padLeft(4f)
            }).row()

            add(statusLabel).padTop(8f).row()

            add(VisTable().apply {
                add(connectButton).width(150f).padRight(6f)
                add(returnButton).width(150f)
            }).padTop(12f)
        }

        addActor(table)

        windowTable.setFillParent(true)
        addActor(windowTable)
    }

    private fun connect() {
        val host = addressField.text.ifBlank { Config.DEFAULT_SERVER_HOST }
        val port = portField.text.toIntOrNull() ?: Config.SERVER_PORT

        GamePref.displayName = nameField.text
        GamePref.save()

        statusLabel.setText("Connecting to $host:$port ...")
        connectButton.isDisabled = true

        val client = ctmn.petals.net.ServerClient()
        val pending = ctmn.petals.net.proto.Welcome()

        client.listener = object : ctmn.petals.net.ServerClient.Listener {
            override fun onWelcome(welcome: ctmn.petals.net.proto.Welcome) {
                onUi {
                    connectButton.isDisabled = false
                    pending.serverName = welcome.serverName
                    pending.mapIds = welcome.mapIds

                    // Hand the live connection to the browser, which keeps it for the session.
                    menuScreen.stage = menuScreen.lobbiesStage.connect(client, host, port, pending)
                }
            }

            override fun onError(error: ctmn.petals.net.proto.ErrorMsg) {
                onUi {
                    connectButton.isDisabled = false
                    statusLabel.setText(error.message)
                }
            }

            override fun onConnectFailed(cause: Throwable?) {
                onUi {
                    connectButton.isDisabled = false
                    statusLabel.setText("Could not connect: ${cause?.message ?: "unknown error"}")
                    client.close()
                }
            }

            override fun onDisconnected() {
                onUi {
                    if (menuScreen.stage !== this@ServerConnectStage) return@onUi
                    connectButton.isDisabled = false
                    statusLabel.setText("Disconnected")
                }
            }
        }

        try {
            client.connect(host, port, GamePref.clientId, GamePref.displayName)
        } catch (e: Exception) {
            connectButton.isDisabled = false
            statusLabel.setText("Could not connect: ${e.message}")
            client.close()
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
