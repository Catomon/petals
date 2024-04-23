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
import ctmn.petals.utils.closeJmDNS
import ctmn.petals.utils.startJmDNSAsClient
import ctmn.petals.widgets.*

class LobbyTypesStage(private val menuScreen: MenuScreen) : Stage(menuScreen.viewport, menuScreen.batch) {

    private val labelVsAi = newLabel("Vs AI", "font_5")
    private val customGameButton = newTextButton("Custom game")

    private val labelLocalMp = newLabel("Local Multiplayer", "font_5")
    private val serverLobbyButton = newTextButton("Host")
    private val ipTextField = VisTextField()
    private val clientLobbyButton = newTextButton("Connect")

    private val returnButton = newTextButton("Return")

    private val table = VisTable()
    private val windowTable = VisTable()

    private val searchHost get() = ipTextField.isEmpty

    init {
//        addListener {
//            if (it is ResetStateEvent) {
//                ipTextField.clearText()
//            }
//
//            false
//        }

        ipTextField.messageText = "Ip Address"
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

            add(labelVsAi)
            row()
            add(customGameButton)
            row()

            add(labelLocalMp).padTop(4f)
            row()
            add(serverLobbyButton)
            row()
            add(ipTextField).width(180f)
            row()
            add(clientLobbyButton)
            row()
            add(returnButton).padTop(8f)

            padBottom(30f)
        }

        windowTable.setFillParent(true)
        addActor(windowTable)

        customGameButton.addChangeListener {
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

        clientLobbyButton.addChangeListener {
            clientLobbyButton.isDisabled = true

            val loadingCover = LoadingCover()
            addActor(loadingCover)

            if (searchHost) {
                clientLobbyButton.addAction(TimeAction {
                    if (it > 5f) {
                        closeJmDNS()

                        clientLobbyButton.isDisabled = false

                        addActor(newNotifyWindow("No local servers found", "Connect"))

                        loadingCover.done()

                        return@TimeAction true
                    }
                    return@TimeAction false
                })
            }

            fun goCustomGameSetupStage() {
                menuScreen.stage =
                    CustomGameSetupStage(menuScreen, CustomGameSetupStage.LobbyType.CLIENT)

                loadingCover.done()
                loadingCover.remove()
                menuScreen.stage.addActor(loadingCover)
            }

            if (searchHost) {
                startJmDNSAsClient { result ->
                    if (result) {
                        this@LobbyTypesStage.addAction(OneAction {
                            clientLobbyButton.actions.clear()
                            clientLobbyButton.isDisabled = false

                            closeJmDNS()

                            goCustomGameSetupStage()
                        })
                    } else {
                        Gdx.app.log(javaClass.simpleName, "Unable to start JmDNS.")

                        clientLobbyButton.clearActions()
                        clientLobbyButton.isDisabled = false

                        addActor(
                            newNotifyWindow(
                                "Unable to start JmDNS.\nCheck your network connection.",
                                "Connect"
                            )
                        )

                        loadingCover.done()
                    }
                }
            } else {
                GameClient.ConnectionData.host = ipTextField.text

                try {
                    goCustomGameSetupStage()
                } catch (e: Exception) {
                    addActor(
                        newNotifyWindow(
                            e.localizedMessage,
                            "Connect"
                        )
                    )
                } finally {
                    clientLobbyButton.isDisabled = false
                    loadingCover.done()
                }
            }
        }

        returnButton.addChangeListener {
            menuScreen.stage = menuScreen.menuStage
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