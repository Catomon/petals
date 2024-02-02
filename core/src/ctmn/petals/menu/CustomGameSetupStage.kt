package ctmn.petals.menu

import com.badlogic.gdx.Gdx
import com.badlogic.gdx.graphics.g2d.Batch
import com.badlogic.gdx.scenes.scene2d.Stage
import com.badlogic.gdx.scenes.scene2d.actions.Actions
import com.badlogic.gdx.scenes.scene2d.actions.DelayAction
import com.badlogic.gdx.scenes.scene2d.ui.WidgetGroup
import com.badlogic.gdx.utils.Array
import com.kotcrab.vis.ui.VisUI
import com.kotcrab.vis.ui.widget.VisImage
import com.kotcrab.vis.ui.widget.VisScrollPane
import com.kotcrab.vis.ui.widget.VisTable
import ctmn.petals.*
import ctmn.petals.actors.actions.OneAction
import ctmn.petals.actors.actions.RepeatAction
import ctmn.petals.actors.actions.TimeAction
import ctmn.petals.ai.EasyAiDuelBot
import ctmn.petals.gameactors.label.LabelActor
import ctmn.petals.level.JsonLevel
import ctmn.petals.multiplayer.ClientPlayScreen
import ctmn.petals.multiplayer.HostPlayScreen
import ctmn.petals.multiplayer.JsonMessage
import ctmn.petals.multiplayer.client.GameClient
import ctmn.petals.multiplayer.client.ResponseListener
import ctmn.petals.multiplayer.client.ServerHandler
import ctmn.petals.multiplayer.json.LobbyState
import ctmn.petals.multiplayer.json.PlayerSlotState
import ctmn.petals.multiplayer.json.serverres.Disconnected
import ctmn.petals.multiplayer.json.serverres.LobbyStateResponse
import ctmn.petals.multiplayer.server.ClientHandler
import ctmn.petals.multiplayer.server.ClientRequestsQueue
import ctmn.petals.multiplayer.server.ClientsController
import ctmn.petals.multiplayer.server.GameServer
import ctmn.petals.player.Player
import ctmn.petals.player.newBluePlayer
import ctmn.petals.playscreen.GameMode
import ctmn.petals.playscreen.GameType
import ctmn.petals.playscreen.NoEnd
import ctmn.petals.playscreen.PlayScreen
import ctmn.petals.utils.*
import ctmn.petals.widgets.*
import io.netty.channel.ChannelFuture
import io.netty.util.concurrent.GenericFutureListener
import java.net.UnknownHostException
import java.util.*

class CustomGameSetupStage(private val menuScreen: MenuScreen, pLobbyType: LobbyType = LobbyType.LOCAL) :
    Stage(menuScreen.viewport, menuScreen.batch) {

    private val localPlayer = newBluePlayer
    private val playerSlots = Array<PlayerSlot>()

    private val mainTable = VisTable()
    private val playersTable = VisTable()

    private val mapPreview = MapPreview()
    private val changeMapButton = newImageButton("change")

    private val addAiButton = newTextButton("Add AI")
    private val fogOfWarButton = newTextButton()

    private val roomProblemDrawable = VisUI.getSkin().getDrawable("room_status_problem")
    private val roomOkDrawable = VisUI.getSkin().getDrawable("room_status_ok")
    private val roomFailDrawable = VisUI.getSkin().getDrawable("room_status_fail")
    private val roomStatusImage = VisImage(roomProblemDrawable)

    private val returnButton = newTextButton("Return")
    private val confirmButton = newTextButton("Confirm").apply { isDisabled = true }

    private var gameMode = GameMode.ALL

    private var isHost = pLobbyType != LobbyType.CLIENT
        set(value) {
            field = value; hostChanged()
        }
    private var isPrivate = false

    val lobbyType: LobbyType = pLobbyType
    private val server: GameServer? = if (lobbyType == LobbyType.SERVER) GameServer().apply { run() } else null
    private val client: GameClient? = if (lobbyType == LobbyType.CLIENT) GameClient().apply { run() } else null

    private val serverManager = LobbyServerManager()

    private val hasFreeSlot get() = playerSlots.firstOrNull { it.player == null } != null
    private val freePlayerId
        get() = "12345678".toMutableList().apply {
            removeAll { char ->
                playerSlots.any {
                    it.player?.id == char.toString().toInt()
                }
            }
        }.first().toString().toInt()

    enum class LobbyType {
        LOCAL, SERVER, CLIENT
    }

    // if Sever
    private var state: LobbyState.State = LobbyState.State.WAITING

    // if Client
    @get:Synchronized
    private var remoteLobbyState: LobbyState? = null
    private var remoteLobbyStateChanged = false

    // server stuff v
    private val clientsMessagesListener by lazy {
        object : ClientRequestsQueue.ClientsMessagesListener {
            override fun onClientMessage(clientId: String, jsonMessage: JsonMessage) {
                when (jsonMessage.id) {
                    "disconnected" -> {
                        // remove client's player from the slot
                        playerSlots.firstOrNull { it.player?.clientId == clientId }?.player = null
                    }
                }
            }
        }
    }

    private val clientsListener by lazy {
        object : ClientsController.ClientsListener {
            override fun onClientIdentified(client: ClientHandler) {
                if (client.clientId == "null") throw RuntimeException("Client id is \"null\"")

                if (hasFreeSlot) {
                    // if player is reconnecting
                    val slot = playerSlots.firstOrNull { it.player?.clientId == client.clientId }?.also { slot ->
                        slot.player = Player("Player${"ABCDEFGH"[freePlayerId - 1]}", freePlayerId, freePlayerId)
                        slot.player!!.clientId = client.clientId

                        slot.button.color.a = 1f    // lost connection
                    }

                    // get free slot and set player for client
                    if (slot == null)
                        playerSlots.firstOrNull { it.player == null }?.also { slot ->
                            slot.player = Player("Player${"ABCDEFGH"[freePlayerId - 1]}", freePlayerId, freePlayerId)
                            slot.player!!.clientId = client.clientId
                        }
                }

                serverManager.sendLobbyState()
            }

            override fun onClientConnected(client: ClientHandler) {

            }

            override fun onClientLostConnection(client: ClientHandler) {
                // if we didn't get "disconnected" message we assume player is lost connection
                // todo wait some time and then remove player
                playerSlots.forEach {
                    if (client.clientId != "null" && it.player?.clientId == client.clientId) {

                        if (it.player != null)
                            it.button.color.a = 0.5f    // lost connection

                        println("[Server Lobby] Player ${client.clientId} lost connection")
                    }
                }
            }
        }
    }
    // server stuff ^

    // client stuff v
    private val clientConnectionListener by lazy {
        GenericFutureListener<ChannelFuture> { future ->
            if (!future.isSuccess) {
                menuScreen.stage = menuScreen.matchSelectionStage

                val e = Exception() //future.exceptionNow()
                when (e) {
                    is UnknownHostException -> menuScreen.stage.addActor(
                        newNotifyWindow(
                            "Unknown host.",
                            "Multiplayer"
                        )
                    )

                    else -> menuScreen.stage.addActor(
                        newNotifyWindow(
                            "Unable to connect to the server.",
                            "Multiplayer"
                        )
                    )
                }
            }
        }
    }

    private val serverResponseListener by lazy {
        object : ResponseListener("everything", false) {

            val client = this@CustomGameSetupStage.client!!

            override fun responseReceived(jsonMessage: JsonMessage) {
                super.responseReceived(jsonMessage)

                println("[ClientLobby] Message received: ${jsonMessage.id}")

                when (jsonMessage.id) {
                    "lobby_state" -> {
                        remoteLobbyState = fromGson(jsonMessage.message, LobbyStateResponse::class.java).lobbyState
                        remoteLobbyStateChanged = true
                    }
                    //                    "client_id" -> {
                    //                        localPlayer.clientId = fromGson(jsonMessage.message, ClientIdMessage::class.java).clientId
                    //                    }
                    "disconnected" -> {
                        client.channelFuture.channel()?.close()

                        val reason = fromGson(jsonMessage.message, Disconnected::class.java).reason
                        menuScreen.menuStage.addActor(
                            newNotifyWindow(
                                "Disconnected from the server. Reason: $reason",
                                "Multiplayer"
                            )
                        )
                        menuScreen.stage = menuScreen.menuStage
                    }

                    "lobby_state_request" -> {
                        serverManager.sendLobbyState()
                    }
                }
            }
        }
    }

    private val serverListener by lazy {
        object : ServerHandler.ServerListener {

            val client = this@CustomGameSetupStage.client!!

            var reconnectCounter = 0

            override fun onIdentified(clientId: String) {

            }

            override fun onConnected() {

            }

            override fun onDisconnected() {
                // return if at this point we are not at this stage assuming that we got "disconnected" message
                if (menuScreen.stage != this@CustomGameSetupStage) return

                // Adding action to try to reconnect, if we are not connected after 3 tries show a notification window
                addAction(
                    Actions.sequence(
                        RepeatAction(1.5f, 3) {
                            reconnectCounter++

                            Gdx.app.log("CustomGameSetupStage", "Reconnecting... $reconnectCounter")

                            roomStatusImage.drawable = roomFailDrawable

                            if (!client.running())
                                client.run()

                            if (client.running() && client.channelFuture.isSuccess) {
                                Gdx.app.log("CustomGameSetupStage", "Reconnected!")
                                reconnectCounter = 0

                                roomStatusImage.drawable = roomOkDrawable

                                //client.clientManager.sendRequest(LobbyStateRequest())

                                return@RepeatAction true
                            }
                            return@RepeatAction false
                        },
                        DelayAction(1.5f),
                        OneAction {
                            if (!client.running() && !client.channelFuture.isSuccess) {
                                menuScreen.menuStage.addActor(
                                    newNotifyWindow(
                                        "Lost connection to the server.",
                                        "Multiplayer"
                                    )
                                )
                                menuScreen.stage = menuScreen.menuStage
                            }
                        })
                )
            }
        }
    }
    // client stuff ^

    init {
        //mapPreview.drawSimpleTexture = true

        with(playersTable) {
            for (i in 1..8) {
                add(PlayerSlot()).size(72f, 72f)
                if (i % 4 == 0)
                    row()
            }
        }

        mainTable.width = 300f
        mainTable.setFillParent(true)
        with(mainTable) {
            add(mapPreview).size(300f).padTop(6f)
            row()
            add(VisScrollPane(VisTable().apply {
                add(playersTable)
                row()
                add(VisTable().apply {
                    add(addAiButton)
                    add(fogOfWarButton)
                })
            }))
            row()
            add().expandY()
            row()
            //add(gameModeSelectBox).center().width(300f)
            //row()
            add(VisTable().apply {
                add(returnButton).left()
                add(roomStatusImage).size(42f).expandX()
                add(confirmButton).right()
            }).expandX()
        }.width(300f)

        addActor(mainTable)

        with(createTable()) {
            top()
            add().width(mapPreview.width - changeMapButton.width).height(mapPreview.height)
            add(changeMapButton).bottom()
        }

        changeMapButton.addChangeListener {
            menuScreen.stage = menuScreen.mapSelectionStage.also {
                it.onResult = { level ->
                    if (level != null) {
                        setLevel(level as JsonLevel)
                    }

                    menuScreen.stage = this

                    serverManager.sendLobbyState()
                }
            }
        }

        returnButton.addChangeListener {
            state = LobbyState.State.CLOSED

            serverManager.sendLobbyState()

            client?.disconnect()

            server?.shutdown()

            menuScreen.stage = menuScreen.menuStage
        }

        confirmButton.addChangeListener {
            addLoadingCoverAndStartGame()
        }

        addAiButton.addChangeListener {
            val slot = playerSlots.firstOrNull { it.player == null }

            if (slot != null) {
                slot.player = Player("Player${"ABCDEFGH"[freePlayerId - 1]}", freePlayerId, freePlayerId)
                slot.isAI = true
            }

            serverManager.sendLobbyState()
        }

        fogOfWarButton.userObject = true
        fogOfWarButton.setText("FOW: " + if (fogOfWarButton.userObject as Boolean) "On" else "Off")
        fogOfWarButton.addChangeListener {
            fogOfWarButton.userObject = !(fogOfWarButton.userObject as Boolean)
            fogOfWarButton.setText("FOW: " + if (fogOfWarButton.userObject as Boolean) "On" else "Off")
        }

        val clientId = GamePref.clientId

        localPlayer.clientId = clientId

        if (isHost) {
            setPlayerSlot(localPlayer, 0)
        }

        // as Client
        client?.let {
            client.clientManager.clientId = clientId
            setupClient()
        }

        // as Server
        server?.let {
            setupServer()
        }

        //..
        hostChanged()
    }

    private fun setupServer() {
        val server = this.server!!

        startJmDNSAsServer { result ->
            if (!result) {
                Gdx.app.log("CustomGameSetupStage", "Unable to start JmDNS.")
//                menuScreen.matchSelectionStage.addActor(
//                    newNotifyWindow(
//                        "Unable to start JmDNS.\nCheck your network connection.",
//                        "Host"
//                    )
//                )
//                menuScreen.stage = menuScreen.matchSelectionStage

                roomStatusImage.drawable = roomFailDrawable
            } else {
                roomStatusImage.drawable = roomOkDrawable
            }
        }

        server.clientRequestsQueue.clientsMessagesListener = clientsMessagesListener

        server.clientsController.clientsListener = clientsListener
    }

    private fun setupClient() {
        val client = this.client!!
        client.channelFuture.addListener(clientConnectionListener)
        client.clientManager.addResponseListener(serverResponseListener)
        client.handler.serverListener = serverListener

        roomStatusImage.drawable = roomOkDrawable
    }

    private var doTime = 0f

    override fun act(delta: Float) {
        super.act(delta)

        // client stuff
        if (remoteLobbyStateChanged && remoteLobbyState != null) {
            setLobbyState(remoteLobbyState!!)
            remoteLobbyStateChanged = false
        }

        //do something once per 2 seconds
        doTime += delta
        if (doTime > 2f) {
            doTime = 0f

            //..
        }
    }

    private fun setLobbyState(state: LobbyState) {
        if (state.level != null) {
            if (state.level != (mapPreview.level as JsonLevel?)?.fileName)
                setLevel(JsonLevel.fromFile(state.level!!))
        } else
            setLevel(null)

        // set local player from remote lobby
        if (localPlayer.clientId != "null" && localPlayer.clientId != null)
            state.players.firstOrNull { it?.player?.clientId == localPlayer.clientId }?.player?.let {
                localPlayer.setFrom(it)
            }

        for (i in 0 until 8) {
            val lobbyStatePlayer = state.players[i]?.player

            if (localPlayer.clientId != "null" && localPlayer.clientId != null && lobbyStatePlayer?.clientId == localPlayer.clientId)
                setPlayerSlot(localPlayer, i)
            else
                setPlayerSlot(lobbyStatePlayer, i)

            val isSlotEmpty = lobbyStatePlayer == null

            playerSlots[i].isHostSlot = !isSlotEmpty && state.players[i]!!.isHost

            playerSlots[i].isAI = !isSlotEmpty && state.players[i]!!.isAI
        }

        fogOfWarButton.userObject = state.fogOfWar
        fogOfWarButton.setText("FOW: " + if (state.fogOfWar) "On" else "Off")

        if (state.state == LobbyState.State.PLAYING)
            addLoadingCoverAndStartGame()
    }

    private fun getLobbyStateInstance(): LobbyState {
        return LobbyState().apply {
            if (mapPreview.level != null)
                level = (mapPreview.level as JsonLevel).fileName

            for ((i, slot) in playerSlots.withIndex())
                if (slot.player != null) {
                    players[i] = slot.getSlotStateInstance()
                }

            fogOfWar = fogOfWarButton.userObject as Boolean

            state = this@CustomGameSetupStage.state
        }
    }

    private fun addLoadingCoverAndStartGame() {
        val loadingCover = LoadingCover()
        loadingCover.fadeOutDelay = 0.75f
        addActor(loadingCover)

        addAction(TimeAction {
            if (it < 0.1f) return@TimeAction false

            startGame()

            loadingCover.done()
            loadingCover.remove()
            (game.screen as PlayScreen).guiStage.addActor(loadingCover)

            true
        })
    }

    private fun startGame() {
        state = LobbyState.State.PLAYING
        serverManager.sendLobbyState()

        if (mapPreview.level == null) return

        with(mapPreview.level as JsonLevel) {
            if (!actorsInitialized)
                initActors(assets)
        }

        val players = Array<Player>()
        playerSlots.forEach { slot ->
            slot.player?.let { players.add(it) }
        }

        val playScreen =
            when (lobbyType) {
                LobbyType.SERVER -> HostPlayScreen(server!!, getLobbyStateInstance())
                LobbyType.CLIENT -> ClientPlayScreen(client!!)
                LobbyType.LOCAL -> PlayScreen()
            }

        val ps = PlayScreenTemplate.pvp(
            menuScreen.game,
            mapPreview.level!!,
            players,
            GameType.MULTIPLAYER,
            NoEnd(),
            gameMode,
            localPlayer,
            playScreen,
        ).apply {
            playerSlots.forEach {
                if (it.isAI)
                    aiManager.add(EasyAiDuelBot(it.player!!, this))
            }

            fogOfWarManager.drawFog = fogOfWarButton.userObject as Boolean
        }
        ps.ready()

        menuScreen.game.screen = ps
    }

    private fun setLevel(level: JsonLevel?) {
        playerSlots.forEach {
            it.label = null
        }

        if (level != null) {
            if (!level.actorsInitialized)
                level.initActors(assets)

            mapPreview.setPreview(level)

            if (level.gameMode.isNotEmpty()) {
                val gm = level.gameMode.toUpperCase(Locale.ROOT)
                if (GameMode.values().any { it.name == gm })
                    gameMode = GameMode.valueOf(gm)
                else
                    Gdx.app.error("CustomGameSetupStage", "Unknown game mode: $gm")
            }

            for (label in level.labels) {
                if (label.labelName == "player") {
                    val id = label.data["id"].toInt()

                    playerSlots.get(id - 1).label = label
                }
            }
        } else {
            mapPreview.setPreview(null)
        }

        mapPreview.createPlayerMarks()

        playerSlots.forEachIndexed { index, slot ->
            slot.player?.let {
                mapPreview.changePlayerMark(it, index + 1)
            }
        }

        confirmButton.isDisabled = level == null || !isHost
    }

    private fun addPlayer(player: Player) {
        for (slot in playerSlots)
            slot.player?.let { slot.player = player }
    }

    private fun setPlayerSlot(player: Player?, slotIndex: Int) {
        if (playerSlots.size <= slotIndex) throw IllegalArgumentException("slotIndex out of bounds")

        playerSlots.get(slotIndex).player = player
    }

    private fun getPlayerSlot(player: Player): PlayerSlot? {
        for (slot in playerSlots)
            if (slot.player == player)
                return slot

        return null
    }

    private fun hostChanged() {
        changeMapButton.isDisabled = !isHost

        addAiButton.isDisabled = !isHost

        confirmButton.isDisabled = mapPreview.level == null || !isHost
    }

    private fun removePlayer(player: Player) {
        if (player.clientId != null) {
            server?.clientsController?.disconnectClient(player.clientId!!, ClientsController.REASON_KICKED)
        }

        for (slot in playerSlots)
            if (slot.player == player)
                slot.player = null

        serverManager.sendLobbyState()

        return
    }

    private inner class PlayerSlot : WidgetGroup() {
        val button = newImageButton("player_slot")
        val removePlayerButton = newImageButton("remove_player")
        var label: LabelActor? = null
            set(value) {
                field = value

                if (player != null)
                    value?.data?.put("player_id", player!!.id.toString())
            }

        val localPlayerMarkImage = VisUI.getSkin().getDrawable("portraits/local_player")
        val hostCrownImage = VisUI.getSkin().getDrawable("portraits/crown")
        val aiPlayerMarkImage = VisUI.getSkin().getDrawable("portraits/ai_player")

        val isLocalHostSlot get() = localPlayer == player && isHost

        var isHostSlot = false
        var isLocalPlayer = false
        var isAI = false

        var player: Player? = null
            set(value) {
                isAI = false

                button.color.a = 1f // lost connection

                if (value != null) {
                    button.style.imageUp = VisUI.getSkin().getDrawable("portraits/${"abcdefgh"[value.id - 1]}")

                    removePlayerButton.isVisible = value != localPlayer && isHost

                    mapPreview.changePlayerMark(value, playerSlots.indexOf(this, false) + 1)

                    label?.data?.put("player_id", value.id.toString())
                } else {
                    button.style.imageUp = null

                    removePlayerButton.isVisible = false

                    mapPreview.changePlayerMark(null, playerSlots.indexOf(this, false) + 1)

                    label?.data?.removeKey("player_id")
                }

                isLocalPlayer = value == localPlayer

                field = value
            }

        init {
            button.addChangeListener {
                if (player != null || !isHost)
                    return@addChangeListener

                getPlayerSlot(localPlayer)?.player = null
                player = localPlayer

                serverManager.sendLobbyState()
            }

            setSize(72f, 72f)

            addActor(button)

            removePlayerButton.addChangeListener {
                if (player == null || !isHost)
                    return@addChangeListener

                removePlayer(player!!)
            }

            removePlayerButton.setPosByCenter(width - 12f, height - 9f)
            removePlayerButton.isVisible = false
            addActor(removePlayerButton)

            playerSlots.add(this)
        }

        override fun draw(batch: Batch, parentAlpha: Float) {
            super.draw(batch, parentAlpha)

            if (isLocalPlayer) {
                localPlayerMarkImage.draw(batch, x, y, localPlayerMarkImage.minWidth, localPlayerMarkImage.minHeight)
            }

            if (isHostSlot || isLocalHostSlot) {
                hostCrownImage.draw(
                    batch,
                    x + width / 2 - hostCrownImage.minWidth / 2,
                    y + height / 2 - hostCrownImage.minHeight / 2,
                    hostCrownImage.minWidth,
                    hostCrownImage.minHeight
                )
            }

            if (isAI) {
                aiPlayerMarkImage.draw(batch, x, y, aiPlayerMarkImage.minWidth, aiPlayerMarkImage.minHeight)
            }
        }

        fun getSlotStateInstance(): PlayerSlotState {
            return PlayerSlotState(player, isLocalHostSlot, isAI)
        }
    }

    private inner class LobbyServerManager {
        fun sendLobbyState() {
            server?.clientsController?.broadcastMessage(JsonMessage(LobbyStateResponse(getLobbyStateInstance()).toGson()))
        }
    }
}