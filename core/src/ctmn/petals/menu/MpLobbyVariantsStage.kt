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
import ctmn.petals.actors.actions.OneAction
import ctmn.petals.actors.actions.TimeAction
import ctmn.petals.multiplayer.client.GameClient
import ctmn.petals.strings
import ctmn.petals.utils.closeJmDNS
import ctmn.petals.utils.startJmDNSAsClient
import ctmn.petals.widgets.*

class MpLobbyVariantsStage(private val menuScreen: MenuScreen) : Stage(menuScreen.viewport, menuScreen.batch) {

    private val labelSameScreen = newLabel(strings.general.same_screen, "default")
    private val passAndPlayButton = newTextButton(strings.general.pass_and_play)

    private val labelLocalMp = newLabel(strings.general.local_multiplayer, "default")
    private val serverLobbyButton = newTextButton(strings.general.host)
    private val ipTextField = VisTextField()

    //private val clientLobbyButton = newTextButton("Connect")
    private val serverSearchButton = newImageButton("server_search")
    private val serverConnectButton = newImageButton("server_connect")

    private val returnButton = newTextButton(strings.general.return_)

    private val table = VisTable()
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

            add(labelSameScreen)
            row()
            add(passAndPlayButton).padBottom(16f)
            row()

            add(labelLocalMp).padTop(4f)
            row()
            add(serverLobbyButton).padBottom(16f).minWidth(100f)
            row()

            add(VisTable().apply {
                add(serverSearchButton)
                add(ipTextField).fillX().expandX()
                add(serverConnectButton)
            }).width(300f)
            row()

            add(returnButton).padTop(24f)

            padBottom(30f)
        }

        windowTable.setFillParent(true)
        addActor(windowTable)

        passAndPlayButton.addChangeListener {
            menuScreen.stage = menuScreen.botGameSetupStage
        }

        serverLobbyButton.addChangeListener {
            val loadingCover = LoadingCover()
            addActor(loadingCover)

            addAction(TimeAction {
                if (it < 0.1f) return@TimeAction false

                Gdx.app.postRunnable {
                    val serverLobby = CustomGameSetupStage(menuScreen, CustomGameSetupStage.LobbyType.SERVER)

                    loadingCover.done()
                    loadingCover.remove()
                    serverLobby.addActor(loadingCover)

                    menuScreen.stage = serverLobby
                }

                true
            })
        }

        serverSearchButton.addChangeListener {
            searchServer()
        }

        serverConnectButton.addChangeListener {
            serverConnect()
        }

        returnButton.addChangeListener {
            menuScreen.stage = menuScreen.menuStage
        }
    }

    private fun searchServer() {
        serverSearchButton.isDisabled = true

        val loadingCover = LoadingCover(strings.general.connecting)
        addActor(loadingCover)



        loadingCover.setLabelText(strings.general.looking_for_server)

        startJmDNSAsClient { result ->
            if (result) {
                this@MpLobbyVariantsStage.addAction(OneAction {
                    serverSearchButton.actions.clear()
                    serverSearchButton.isDisabled = false

                    closeJmDNS()

                    menuScreen.stage =
                        CustomGameSetupStage(menuScreen, CustomGameSetupStage.LobbyType.CLIENT)

                    loadingCover.done()
                    loadingCover.remove()
                    menuScreen.stage.addActor(loadingCover)
                })
            } else {
                Gdx.app.log(javaClass.simpleName, "Unable to start JmDNS.")

                serverSearchButton.clearActions()
                serverSearchButton.isDisabled = false

                addActor(
                    newNotifyWindow(
                        "Unable to start JmDNS.\n${strings.general.check_your_network_connection}",
                        strings.general.connect
                    )
                )

                loadingCover.done()
            }
        }

        // close jmdns after 5 secs
        serverSearchButton.addAction(TimeAction {
            if (it > 5f) {
                closeJmDNS()

                serverSearchButton.isDisabled = false

                addActor(newNotifyWindow(strings.general.no_local_servers_found, strings.general.connect))

                loadingCover.done()

                return@TimeAction true
            }
            return@TimeAction false
        })
    }

    private fun serverConnect() {
        serverConnectButton.isDisabled = true

        val loadingCover = LoadingCover(strings.general.connecting)
        addActor(loadingCover)

        GameClient.ConnectionData.host = ipTextField.text.ifEmpty { "localhost" }

        try {
            menuScreen.stage =
                CustomGameSetupStage(menuScreen, CustomGameSetupStage.LobbyType.CLIENT)

            loadingCover.done()
            loadingCover.remove()
            menuScreen.stage.addActor(loadingCover)
        } catch (e: Exception) {
            addActor(
                newNotifyWindow(
                    e.localizedMessage,
                    strings.general.connect
                )
            )
        } finally {
            serverConnectButton.isDisabled = false
            loadingCover.done()
        }
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