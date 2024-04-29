package ctmn.petals.multiplayer

import com.badlogic.gdx.Gdx
import com.badlogic.gdx.scenes.scene2d.actions.Actions
import com.badlogic.gdx.scenes.scene2d.actions.DelayAction
import ctmn.petals.screens.MenuScreen
import ctmn.petals.actors.actions.OneAction
import ctmn.petals.actors.actions.RepeatAction
import ctmn.petals.multiplayer.client.ClientManager.Companion.MESSAGE_ID_EVERYTHING
import ctmn.petals.multiplayer.client.GameClient
import ctmn.petals.multiplayer.client.ResponseListener
import ctmn.petals.multiplayer.client.ServerHandler
import ctmn.petals.multiplayer.json.clientreq.ExecuteCommand
import ctmn.petals.multiplayer.json.clientreq.GameStateRequest
import ctmn.petals.multiplayer.json.clientreq.RandomGeneratorRequest
import ctmn.petals.multiplayer.json.serverres.GameStateResponse
import ctmn.petals.multiplayer.json.serverres.RandomGeneratorResponse
import ctmn.petals.multiplayer.json.serverres.StatusRequest
import ctmn.petals.multiplayer.json.serverres.StatusResponse
import ctmn.petals.playscreen.PlayScreen
import ctmn.petals.playscreen.addAction
import ctmn.petals.playscreen.commands.Command
import ctmn.petals.playscreen.events.CommandExecutedEvent
import ctmn.petals.playscreen.gui.floatingLabel
import ctmn.petals.utils.fromGson
import ctmn.petals.utils.log
import ctmn.petals.utils.toGson
import ctmn.petals.widgets.newNotifyWindow

class ClientPlayScreen(
    private val gameClient: GameClient,
) : PlayScreen() {

    private var serverStatus = ServerStatus.NOT_READY

    private var randomGeneratorObtained = false

    private enum class ServerStatus {
        NOT_READY, READY, SHUTDOWN
    }

    private val maxReconnects = 3

    private val serverListener by lazy {
        object : ServerHandler.ServerListener {
            val client = gameClient

            var reconnectCounter = 0

            override fun onIdentified(clientId: String) {
                //
            }

            override fun onConnected() {
                //
            }

            override fun onDisconnected() {
                // return if at this point we are not at this stage assuming that we got "disconnected" message
                if (game.screen != this@ClientPlayScreen) return

                if (serverStatus == ServerStatus.SHUTDOWN) {
                    log("Disconnected due to server shutdown.")
                    return
                }

                // Adding action to try to reconnect, if we are not connected after 3 tries show a notification window
                guiStage.addAction(
                    Actions.sequence(
                        RepeatAction(1.5f, maxReconnects) {
                            reconnectCounter++

                            Gdx.app.log("CustomGameSetupStage", "Reconnecting... $reconnectCounter")

                            guiStage.floatingLabel("Reconnecting... $reconnectCounter")

                            // todo I guess GameClient should make isRunning == false when disconnected
                            // it was so before but after addition of port incrementing
                            // fixme running() returns true even if disconnected
                            //if (!client.running())
                            client.run()

                            if (client.running() && client.channelFuture.isSuccess) {
                                Gdx.app.log("CustomGameSetupStage", "Reconnected!")
                                guiStage.floatingLabel("Reconnected!")

                                reconnectCounter = 0

                                //client.clientManager.sendRequest(LobbyStateRequest())

                                return@RepeatAction true
                            }
                            return@RepeatAction false
                        },
                        DelayAction(1.5f),
                        OneAction {
                            if (!client.running() && !client.channelFuture.isSuccess) {
                                log("Lost connection to the server.")

                                returnToMenuScreen()
                                val menuScreen = (game.screen as MenuScreen)
                                menuScreen.stage.addActor(
                                    newNotifyWindow("Lost connection to the server.", "Multiplayer")
                                )
                            }
                        })
                )
            }
        }
    }

    override fun ready() {
        super.ready()

        gameClient.handler.serverListener = serverListener

        gameClient.clientManager.addResponseListener(object : ResponseListener(MESSAGE_ID_EVERYTHING, false) {
            override fun responseReceived(jsonMessage: JsonMessage) {
                when (jsonMessage.id) {
                    "status_request" -> {
                        gameClient.clientManager.sendMessage(StatusResponse("ready").toJsonMessage())
                    }

                    "status" -> {
                        val status = fromGson(jsonMessage.message, StatusResponse::class.java)
                        serverStatus = when (status.status) {
                            "ready" -> ServerStatus.READY
                            else -> ServerStatus.NOT_READY
                        }
                    }

                    "game_state" -> {
                        val gameStateResponse = fromGson(jsonMessage.message, GameStateResponse::class.java)

                        /** actions are getting cleared in [applyGameStateToPlayScreen]
                         * just a reminder */
                        addAction {
                            applyGameStateToPlayScreen(
                                gameStateResponse.gameState,
                                this@ClientPlayScreen,
                                isMultiplayer = true
                            )
                        }
                    }
                    // execute command that was sent by server
                    // but only if the command is for our game state
                    // (command.gameStateId - 1 == gameStateId)
                    // if command.gameStateId is equal to our gameStateId, then we don't need to execute it
                    // if command.gameStateId is 2+ more than our gameStateId, then we need to request new game state
                    // because it means the server gameStateId is 1 command ahead of ours
                    "execute_command" -> {
                        val executeCommand = fromGson(jsonMessage.message, ExecuteCommand::class.java)

                        // check if command is executable for our game state
                        when {
                            executeCommand.gameStateId - 1 == gameStateId -> {
                                Gdx.app.log(
                                    "ClientPlayScreen",
                                    "Server sent a command executable for our game state (c:$gameStateId,  s:${executeCommand.gameStateId}). Executing it."
                                )

                                val commandClass =
                                    Class.forName("ctmn.petals.playscreen.commands.${executeCommand.commandName}")
                                val command = fromGson(executeCommand.command, commandClass) as Command

                                commandManager.queueCommand(command)
                            }

                            executeCommand.gameStateId > gameStateId -> {
                                Gdx.app.log(
                                    "ClientPlayScreen",
                                    "Server sent a command for a different game state (c:$gameStateId,  s:${executeCommand.gameStateId}). Requesting new game state."
                                )

                                gameClient.clientManager.sendMessage(GameStateRequest())
                            }

                            executeCommand.gameStateId == gameStateId -> {
                                Gdx.app.log(
                                    "ClientPlayScreen",
                                    "Server send command with the same game state (c:$gameStateId,  s:${executeCommand.gameStateId}). If it is our command, all fine. No need to execute it."
                                )
                            }
                        }
                    }

                    "random_generator" -> {
                        val randomGen = fromGson(jsonMessage.message, RandomGeneratorResponse::class.java)
                        randomSeed = randomGen.randomSeed
                        randomCount = randomGen.randomCount

                        randomGeneratorObtained = true
                    }

                    "disconnected" -> {
                        serverStatus = ServerStatus.SHUTDOWN

                        if (!isGameOver) {
                            guiStage.addAction(OneAction {
                                returnToMenuScreen()
                                val menuScreen = (game.screen as MenuScreen)
                                menuScreen.stage.addActor(
                                    newNotifyWindow("Server closed.", "Multiplayer")
                                )
                            })
                        }
                    }
                }
            }
        })

        // wait for server to be ready
        while (serverStatus != ServerStatus.READY && !randomGeneratorObtained) {
            Thread.sleep(1000)

            gameClient.clientManager.sendMessage(StatusRequest().toJsonMessage())
            gameClient.clientManager.sendMessage(RandomGeneratorRequest().toJsonMessage())
        }

        // send ready status
        gameClient.clientManager.sendMessage(StatusResponse("ready").toJsonMessage())
    }

    override fun initGui() {
        super.initGui()
        // every time we execute a command, we send it to the server
        // but only if it is our turn
        guiStage.addListener {
            if (it is CommandExecutedEvent) {
                if (it.command.playerId == localPlayer.id) {
                    Gdx.app.log("ClientPlayScreen", "Sending command ${it.command.javaClass.simpleName}")

                    val command = it.command
                    gameClient.clientManager.sendMessage(
                        ExecuteCommand(command.javaClass.simpleName, command.toGson(), gameStateId).toJsonMessage()
                    )
                }
            }
            false
        }
    }

    fun requestGameState() {
        gameClient.clientManager.sendMessage(GameStateRequest())
    }

    fun disconnectFromServer() {
        gameClient.disconnect()
    }
}