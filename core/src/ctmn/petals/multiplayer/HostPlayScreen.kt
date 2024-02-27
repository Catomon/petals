package ctmn.petals.multiplayer

import com.badlogic.gdx.Gdx
import com.badlogic.gdx.Input
import ctmn.petals.actors.actions.UpdateAction
import ctmn.petals.multiplayer.json.GameStateSnapshot
import ctmn.petals.multiplayer.json.LobbyState
import ctmn.petals.multiplayer.json.clientreq.ExecuteCommand
import ctmn.petals.multiplayer.json.serverres.*
import ctmn.petals.multiplayer.server.ClientHandler
import ctmn.petals.multiplayer.server.ClientRequestsQueue
import ctmn.petals.multiplayer.server.ClientsController
import ctmn.petals.multiplayer.server.GameServer
import ctmn.petals.playscreen.PlayScreen
import ctmn.petals.playscreen.addAction
import ctmn.petals.playscreen.commands.Command
import ctmn.petals.playscreen.events.CommandExecutedEvent
import ctmn.petals.playscreen.seqactions.SeqAction
import ctmn.petals.screens.MenuScreen
import ctmn.petals.utils.fromGson
import ctmn.petals.utils.toGson
import ctmn.petals.widgets.LoadingCover
import ctmn.petals.widgets.newNotifyWindow

class HostPlayScreen(
    private val gameServer: GameServer,
    private val lobbyState: LobbyState,
) : PlayScreen() {

    private val playerStatuses = mutableMapOf<String, PlayerStatus>()

    private enum class PlayerStatus {
        READY, NOT_READY, AFK, LOST_CONNECTION, DISCONNECTED
    }

    override fun ready() {
        super.ready()

        // set all players statuses to not ready
        for (player in turnManager.players) {
            if (player.clientId != null && player.clientId != "null")
                playerStatuses[player.clientId!!] = PlayerStatus.NOT_READY
        }
        // set local player status to ready
        playerStatuses[localPlayer.clientId!!] = PlayerStatus.READY

        //..
        gameServer.clientsController.clientsListener = object : ClientsController.ClientsListener {
            override fun onClientIdentified(client: ClientHandler) {
                super.onClientIdentified(client)

                // todo prob should wait till client loads into the game
                playerStatuses[client.clientId] = PlayerStatus.READY

                broadcastMessage(JsonMessage(LobbyStateResponse(lobbyState).toGson()))
            }

            override fun onClientDisconnected(client: ClientHandler) {
                super.onClientDisconnected(client)

                playerStatuses[client.clientId] = PlayerStatus.DISCONNECTED
            }

            override fun onClientLostConnection(client: ClientHandler) {
                super.onClientLostConnection(client)

                playerStatuses[client.clientId] = PlayerStatus.LOST_CONNECTION

                val loadingCover = LoadingCover()
                guiStage.addActor(loadingCover)

                addAction(object : SeqAction() {
                    override fun update(deltaTime: Float) {
                        if (playerStatuses.values.firstOrNull { it != PlayerStatus.READY } == null) {
                            loadingCover.done()
                            isDone = true
                        }
                    }

                    override fun onStart(playScreen: PlayScreen): Boolean {
                        lifeTime = 60f

                        return true
                    }
                })
            }
        }

        gameServer.clientRequestsQueue.clientsMessagesListener = object : ClientRequestsQueue.ClientsMessagesListener {
            override fun onClientMessage(clientId: String, jsonMessage: JsonMessage) {
                when (jsonMessage.id) {
                    "status_request" -> {
                        sendMessage(clientId, StatusResponse("ready"))
                    }

                    "status" -> {
                        val status = fromGson(jsonMessage.message, StatusResponse::class.java)
                        when (status.status) {
                            "ready" -> {
                                if (playerStatuses[clientId] != PlayerStatus.NOT_READY) {
                                    sendGameState(clientId)
                                }

                                playerStatuses[clientId] = PlayerStatus.READY
                            }
                        }
                    }

                    "game_state_request" -> {
                        sendGameState(clientId)
                    }

                    "execute_command" -> {
                        val executeCommand = fromGson(jsonMessage.message, ExecuteCommand::class.java)
                        val commandClass =
                            Class.forName("ctmn.petals.playscreen.commands.${executeCommand.commandName}")
                        val command = fromGson(executeCommand.command, commandClass) as Command

                        commandManager.queueCommand(command)
                    }

                    "random_generator_request" -> {
                        sendMessage(clientId, RandomGeneratorResponse(randomSeed, randomCount))
                    }
                }
            }
        }

        // wait for all players to be ready
        while (playerStatuses.values.firstOrNull { it != PlayerStatus.READY } != null) {

            Thread.sleep(1000)

            broadcastMessage(StatusRequest())

            val playersReady = playerStatuses.values.filter { it == PlayerStatus.READY }

            Gdx.app.log("HostPlayScreen", "Waiting for players to be ready ${playerStatuses.size}/${playersReady.size}")

            var allDisconnected = true
            for ((i, playerStatus) in playerStatuses.values.withIndex()) {
                if (playerStatuses[localPlayer.clientId] != playerStatuses.values.elementAt(i)
                    && playerStatus != PlayerStatus.LOST_CONNECTION && playerStatus != PlayerStatus.DISCONNECTED)
                    allDisconnected = false
            }

            if (allDisconnected) {
                break

//                returnToMenuScreen()
//                (game.screen as MenuScreen).stage.addActor(
//                    newNotifyWindow("All players disconnected", "Custom game")
//                )
            }
        }
        Gdx.app.log("HostPlayScreen", "All players are ready")

        // for testing
        playStage.addAction(UpdateAction {
            if (Gdx.input.isKeyJustPressed(Input.Keys.NUM_2))
                gameStateId++

            false
        })
    }

    /** Gui Stage is getting destroyed after applying the requested state to PlayScreen
     * see [ctmn.petals.multiplayer.applyGameStateToPlayScreen] */
    override fun initGui() {
        super.initGui()

        // send all executed commands to all players
        guiStage.addListener {
            if (it is CommandExecutedEvent) {
                val command = it.command
                broadcastMessage(ExecuteCommand(command.javaClass.simpleName, command.toGson(), gameStateId))

                Gdx.app.log("HostPlayScreen", "Broadcasting command ${command.javaClass.simpleName}")
            }
            false
        }
    }

    fun sendGameState(clientId: String) {
        sendMessage(clientId, GameStateResponse(GameStateSnapshot(this@HostPlayScreen)))
    }

    fun sendMessage(clientId: String, messageObject: Any) {
        gameServer.clientsController.sendMessage(
            clientId,
            if (messageObject is JsonMessage) messageObject else messageObject.toJsonMessage()
        )
    }

    fun broadcastMessage(messageObject: Any) {
        gameServer.clientsController.broadcastMessage(
            if (messageObject is JsonMessage)
                messageObject
            else
                messageObject.toJsonMessage()
        )
    }

    fun shutdownServer() {
        gameServer.shutdown()
    }
}