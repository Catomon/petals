package ctmn.petals.playscreen

import com.badlogic.gdx.*
import com.badlogic.gdx.graphics.Color
import com.badlogic.gdx.graphics.GL20
import com.badlogic.gdx.graphics.OrthographicCamera
import com.badlogic.gdx.graphics.Texture
import com.badlogic.gdx.graphics.g2d.Sprite
import com.badlogic.gdx.graphics.g2d.SpriteBatch
import com.badlogic.gdx.math.Vector2
import com.badlogic.gdx.scenes.scene2d.Action
import com.badlogic.gdx.scenes.scene2d.Event
import com.badlogic.gdx.scenes.scene2d.EventListener
import com.badlogic.gdx.scenes.scene2d.InputEvent
import com.badlogic.gdx.scenes.scene2d.actions.Actions
import com.strongjoshua.console.CommandExecutor
import com.strongjoshua.console.LogLevel
import com.strongjoshua.console.annotation.ConsoleDoc
import ctmn.petals.*
import ctmn.petals.bot.BotManager
import ctmn.petals.bot.EasyDuelBot
import ctmn.petals.effects.FloatingUpLabel
import ctmn.petals.map.MapConverted
import ctmn.petals.map.labels
import ctmn.petals.map.tiles
import ctmn.petals.map.units
import ctmn.petals.multiplayer.ClientPlayScreen
import ctmn.petals.multiplayer.HostPlayScreen
import ctmn.petals.multiplayer.applyGameStateToPlayScreen
import ctmn.petals.multiplayer.createSnapshot
import ctmn.petals.multiplayer.json.GameStateSnapshot
import ctmn.petals.player.Player
import ctmn.petals.playscreen.commands.Command
import ctmn.petals.playscreen.commands.CommandManager
import ctmn.petals.playscreen.commands.EndTurnCommand
import ctmn.petals.playscreen.commands.GrantXpCommand
import ctmn.petals.playscreen.events.*
import ctmn.petals.playscreen.gui.GameOverMenu
import ctmn.petals.playscreen.gui.PlayGUIStage
import ctmn.petals.playscreen.gui.PlayStageCameraController
import ctmn.petals.playscreen.gui.widgets.FogOfWarDrawer
import ctmn.petals.playscreen.gui.widgets.MarkersDrawer
import ctmn.petals.playscreen.listeners.ActionEffectListener
import ctmn.petals.playscreen.listeners.ItemPickUpListener
import ctmn.petals.playscreen.listeners.PinkSlimeLingHealing
import ctmn.petals.playscreen.listeners.TileLifeTimeListener
import ctmn.petals.playscreen.seqactions.SeqActionManager
import ctmn.petals.playscreen.seqactions.ThrowUnitAction
import ctmn.petals.playscreen.tasks.TaskManager
import ctmn.petals.playscreen.triggers.TriggerManager
import ctmn.petals.playstage.*
import ctmn.petals.screens.MenuScreen
import ctmn.petals.screens.pvp.newPvPAlice
import ctmn.petals.story.gameOverFailure
import ctmn.petals.story.gameOverSuccess
import ctmn.petals.tile.*
import ctmn.petals.unit.*
import ctmn.petals.unit.actors.Dummy
import ctmn.petals.unit.component.BurningComponent
import ctmn.petals.unit.component.TileEffectComponent
import ctmn.petals.utils.*
import ctmn.petals.widgets.newLabel
import playScreen.PlayTurnCycleListener
import kotlin.random.Random

open class PlayScreen(
    val game: PetalsGame = ctmn.petals.game,
) : Screen {

    /** Just telling if we ever gonna init [guiStage] */
    var initView = true

    val batch = SpriteBatch()
    val assets = game.assets

    var map: MapConverted? = null
    var levelId: String? = null

    val unitsData = Units
    val tilesData = TileData

    val playStage = PlayStage(batch)
    lateinit var guiStage: PlayGUIStage

    /** Add new processors in [initGui] */
    val inputMultiplexer = InputMultiplexer()

    lateinit var localPlayer: Player

    val turnManager = TurnManager(this)
    val commandManager = CommandManager(this)
    val taskManager = TaskManager(this)
    val triggerManager = TriggerManager(this)
    val actionManager = SeqActionManager(this)
    val botManager = BotManager(this)

    val fogOfWarManager = FogOfWarDrawer(this)

    val playStageCameraController = PlayStageCameraController(playStage)

    var friendlyFire = false

    var creditsPassiveIncome = 100
    var creditsPassiveIncomeMax = 2000
    var creditsPerBase = 100
    var creditsPerCluster = 100 //classic 75

    var season = Season.SUMMER
    var gameType = GameType.STORY
    var gameMode = GameMode.STORY
    var gameEndCondition: GameEndCondition = CaptureBases()
    var isGameOver = false

    var gameStateId = 1

    var randomCount = 0
    var randomSeed = Random.nextInt(0, 9999999)
        set(value) {
            random = Random(value)
            randomCount = 0
            field = value
        }
    var random = Random(randomSeed)
        get() {
            randomCount++
            return field
        }

    private val backImage = TiledBackground(Sprite(Texture("background_tile.png")))

    private val eventLogger = EventLogger(this)
    var debug = false

    private val debugKeysProcessor by lazy { DebugKeysProcessor() }

    private val ambienceMusic =
        AudioManager.music("very_quiet_morning_meadow_forest.ogg").apply { isLooping = true; play(); volume *= 2f }

    init {
        discordRich(Rich.PLAYING)

        playStageSetup()

        commandManager.onCommand = CommandManagerOnCommandHandler()::onCommand
    }

    private fun playStageSetup() {
        playStage.camera.position.x = Gdx.graphics.width / 2f
        playStage.camera.position.y = Gdx.graphics.height / 2f

        playStage.addActorAfterTiles(fogOfWarManager)
        playStage.root.addActorAfter(fogOfWarManager, MarkersDrawer(this))

        // stop touch events if an action is processing
        playStage.addCaptureListener(TouchEventsBlockOnActionListener())

        // game logic
        playStage.addListener(PlayTurnCycleListener(this))
        playStage.addListener(EndConditionListener())

        // less important game logic
        playStage.addListener(TileLifeTimeListener(playStage))
        playStage.addListener(PinkSlimeLingHealing())
        playStage.addListener(ActionEffectListener(this))
        playStage.addListener(ItemPickUpListener(this))
        playStage.addListener {
            if (it is UnitMovedEvent) {
                val unit = it.unit
                val tile = playStage.getTile(unit.tiledX, unit.tiledY)
                if (!unit.isAir && tile != null) {
                    when {
                        tile.terrain == TerrainNames.lava -> {
                            unit.dealDamage(Damage.LAVA, playScreen = this@PlayScreen)
                            unit.add(TileEffectComponent(turnManager.currentPlayer.id))
                        }

                        tile.isBurning -> {
                            unit.dealDamage(Damage.BURN, playScreen = this@PlayScreen)
                            unit.add(BurningComponent(turnManager.currentPlayer.id))
                        }

                        tile.terrain == TerrainNames.chasm -> {
                            unit.dealDamage(Damage.CHASM, playScreen = this@PlayScreen)
                            unit.add(TileEffectComponent(turnManager.currentPlayer.id))
                        }

                        else -> unit.del(TileEffectComponent::class.java)
                    }
                }
            }

            if (it is ActionCompletedEvent) {
                checkGameEndCondition()
            }

            false
        }

        playStage.addListener {
            when (it) {
                is UnitMovedEvent -> {
                    it.unit.updateView()
                }

                is ActionStartedEvent -> {
                    if (it.action is ThrowUnitAction)
                        it.action.unit.updateView()
                }

                is ActionCompletedEvent -> {
                    if (it.action is ThrowUnitAction)
                        it.action.unit.updateView()
                }
            }

            false
        }
    }

    fun update(delta: Float) {
        playStageCameraController.update(delta)

        actionManager.update(delta)
        botManager.update(delta)
        commandManager.update(delta)
        taskManager.update(delta)
        triggerManager.update(delta)
    }

    fun clear() {

    }

    fun setLevel(map: MapConverted) {
        assets.tilesAtlas = when (season) {
            Season.WINTER -> assets.tilesWinterAtlas
            else -> assets.tilesSummerAtlas
        }

        this.map = map
        this.levelId = map.mapId

        for (tile in sortTiles(map.tiles)) {
            playStage.addActor(tile)
        }

        for (unit in map.units) {
            playStage.addActor(unit)
        }

        for (label in map.labels) {
            playStage.addActor(label)
        }

        Decorator.decorate(playStage)

        prepareLevel()

        levelCreated()
    }

    private fun prepareLevel() {
        // if first tile on a tiled position has layer != 1, shift all tiles layer on the position to make it 1
        // like 0 1 2 -> -1 0 1
        // only if layer is <3, in this case: 0 1 2 3 -> -1 0 1 3
        val tiles = playStage.getAllTiles().toMutableList()
        val tilesSamePos = mutableListOf<TileActor>()
        for (x in 0 until playStage.tiledWidth) {
            for (y in 0 until playStage.tiledHeight) {
                tilesSamePos.clear()
                tiles.forEach { if (it.tiledX == x && it.tiledY == y) tilesSamePos.add(it) }
                tilesSamePos.sortByDescending { it.layer }
                if (tilesSamePos.isEmpty()) continue

                if (tilesSamePos.size == 1) {
                    tilesSamePos.first().layer = 1
                    continue
                }

                if (tilesSamePos.firstOrNull { it.layer == 2 } == null) continue

                val filtered = tilesSamePos.filter { it.layer < 3 }
                if (filtered.find { it.layer == 2 } != null) {
                    filtered.forEach {
                        it.layer -= 1
                    }
                }
            }
        }

        val bluCrystals = mutableListOf<TileActor>()
        tiles.removeAll {
            val isLifeCrystal = it.selfName == Tiles.LIFE_CRYSTAL
            if (isLifeCrystal) {
                bluCrystals.add(TileActor(TileData.get(Tiles.CRYSTAL)!!, it.layer, it.tiledX, it.tiledY))
            }

            isLifeCrystal
        }

        tiles.addAll(bluCrystals)

        // re-add tiles to create missing layers
        playStage.clearTiles()
        tiles.forEach { playStage.addActor(it) }
    }

    fun levelCreated() {
        playStage.border.make()
        fogOfWarManager.updateGridMap()
        botManager.botPlayers.forEach { it.levelCreated(playStage) }
    }

    var isReady = false
        protected set

    open fun ready() {
        if (levelId == null) throw IllegalStateException("Level is null")

        if (gameType == GameType.PVP_SAME_SCREEN) {
            localPlayer = if (botManager.isBotPlayer(turnManager.currentPlayer)) turnManager.players.first {
                !botManager.isBotPlayer(it)
            } else turnManager.currentPlayer
        }

        if (map != null)
            assignPlayersToSpawnPoints()

        if (!::guiStage.isInitialized) initGui()

        // add leaders if CRYSTALS_LEADERS mode
        for (label in playStage.getLabels()) {
            if (label.labelName.startsWith("leader_spawn_point")) {
                if (gameMode == GameMode.CRYSTALS_LEADERS) {
                    queueAddUnitAction(newPvPAlice.apply {
                        playerId = playerIdByColor(label.selfName.removePrefix("leader_spawn_point_"))
                        teamId = playerId
                        position(label)
                    })
                }
            }
        }

        // give first turn player gold for all bases
        val currentPlayer = turnManager.currentPlayer
        for (base in playStage.getCapturablesOf(currentPlayer))
            currentPlayer.credits += creditsPerBase

        // ready to show the screen
        isReady = true
        fireEvent(NextTurnEvent(turnManager.currentPlayer, turnManager.currentPlayer))
        /** checkGameEndCondition() <- not needed here cuz [EndConditionListener] calls it on NextTurnEvent */
    }

    /** Should be called after TileData, Units, Players, and CurrentPlayer were added to playStage */
    open fun initGui() {
        if (::guiStage.isInitialized) throw IllegalStateException("${guiStage::class.simpleName} is already initialized.")

        inputMultiplexer.processors.clear()

        guiStage = PlayGUIStage(this)

        inputMultiplexer.addProcessor(guiStage)
        inputMultiplexer.addProcessor(playStage)
        inputMultiplexer.addProcessor(debugKeysProcessor)

        Gdx.input.inputProcessor = inputMultiplexer
    }

    fun setDefaultPlayerIdToLabels() {
        playStage.getLabels().forEach { label ->
            if (label.labelName == "player") {
                label.data.put("player_id", (label.data["id"].toInt() + 1).toString())
            }
        }
    }

    private fun assignPlayersToSpawnPoints() {
        val map = this.map ?: throw IllegalStateException("map == null")

        // set empty bases to life crystals and assign player_id to other bases based on what label is on top of them
        for (label in playStage.getLabels().filter { it.selfName == "player" }) {
            val playerId = label.data["player_id"]?.toInt()

            val base = map.playerBases.firstOrNull {
                it.tiledX == label.tiledX && it.tiledY == label.tiledY
            } ?: continue

            if (playerId != null) {
                setPlayerForCapturableTile(
                    base,
                    playerId,
                    turnManager.getPlayerById(playerId)?.species
                        ?: throw IllegalStateException("Player with playerId<$playerId> assigned to player label not found in turnManager")
                )
            } else {
                setPlayerForCapturableTile(base, -1)
            }

            if (gameType != GameType.STORY) {
                if (base.selfName == Tiles.LIFE_CRYSTAL) {
                    base.remove()
                    val bluTile = TileActor(TileData.get(Tiles.CRYSTAL)!!, base.layer, base.tiledX, base.tiledY)
                    playStage.addActor(bluTile)
                }
            }
        }

        //        when (gameMode) {
//            GameMode.CRYSTALS -> {
//                for (label in playStage.getLabels()) {
//                    if (label.labelName == "player") {
//                        val playerId = label.data["player_id"] ?: continue
//                        val player = turnManager.players.firstOrNull { it.id == playerId.toInt() } ?: continue
//
//                        // label.data["leader_unit_name"]
//                        newPvPAlice.addToStage(playStage).player(player).position(label)
//                            .leader(MathUtils.random(0, 9999999))
//                    }
//                }
//            }
//
//            GameMode.CASTLES -> {
//                var playerCount = 0
//                for (base in playStage.getTiles().filter { it.name.contains("base") && it.cPlayerId != null }) {
//
//                    if (turnManager.players.size == playerCount) break
//
//                    base.cPlayerId!!.playerId = turnManager.players[playerCount].id
//
//                    playerCount++
//                }
//
//                for (i in playerCount until turnManager.players.size) {
//                    turnManager.players[i].isOutOfGame = true
//                }
//            }
//
//            else -> {}
//        }

    }

    override fun render(delta: Float) {
        //UPDATE//
        update(delta)
        playStage.act(delta)
        guiStage.act(delta)

        //RENDER//
        Gdx.gl.glClearColor(0.5f, 0.5f, 0.5f, 1f)
        Gdx.gl.glClear(GL20.GL_COLOR_BUFFER_BIT)

        batch.color = Color.WHITE
        playStage.camera.update()
        batch.projectionMatrix = playStage.camera.combined
        batch.begin()

        backImage.draw(batch, playStage.camera as OrthographicCamera)

        batch.end()

        playStage.draw()

        guiStage.draw()

        RectRenderer.isDrawing = debug
        RectRenderer.shapeRenderer.projectionMatrix = playStage.camera.combined
        RectRenderer.render()
    }

    fun returnToMenuScreen() {
        if (gameType == GameType.MULTIPLAYER) {
            when (this) {
                is HostPlayScreen -> {
                    shutdownServer()
                }

                is ClientPlayScreen -> {
                    disconnectFromServer()
                }
            }
        }

        game.screen = MenuScreen(game)
    }

    private var gameEnding = false

    private fun checkGameEndCondition() {
        if (isGameOver || gameEnding) return

        for (player in turnManager.players) {
            player.isOutOfGame =
                player.isOutOfGame || gameEndCondition.checkPlayerOutOfGame(player, this@PlayScreen) == true
        }

        if (gameEndCondition.check(this)) {
            gameEnding = true

            queueAction {
                val labelGameOver = newLabel("GAME OVER").also { it.isVisible = false }
                labelGameOver.setFontScale(2f)

                val winners = gameEndCondition.winners
                val youWon = winners.contains(localPlayer.id)
                val draw = winners.size == 0
                val enemyWon = !draw && !youWon
                val oneWinner = winners.size == 1

                logMsg("Game Over. Winners: ${winners.joinToString()}; teamId: ${turnManager.getPlayerById(winners.firstOrNull() ?: -1)?.teamId ?: ""}")

                when {
                    youWon -> {
                        labelGameOver.setText("YOU WON")
                        if (oneWinner) {

                        } else {

                        }
                    }

                    enemyWon -> {
                        labelGameOver.setText("YOU LOSE")
                        if (oneWinner) {

                        } else {

                        }
                    }

                    draw -> {
                        labelGameOver.setText("DRAW")
                    }
                }

                guiStage.addActor(labelGameOver)
                labelGameOver.setPosition(
                    guiStage.camera.position.x - labelGameOver.width / 2,
                    guiStage.camera.position.y
                )
                labelGameOver.isVisible = true
                labelGameOver.addAction(Actions.delay(3f, object : Action() {
                    override fun act(delta: Float): Boolean {
                        labelGameOver.remove()
                        gameOver()
                        return true
                    }
                }))
            }
        }
    }

    fun gameOver() {
        if (isGameOver) return

        gameEnding = true
        isGameOver = true
        fireEvent(GameOverEvent())

        onGameOver()
    }

    open fun onGameOver() {
        val winners = gameEndCondition.winners
        val youWon = winners.contains(localPlayer.id)
        val draw = winners.size == 0
        val enemyWon = !draw && !youWon
        val oneWinner = winners.size == 1

        val message = when {
            youWon -> {
                if (oneWinner) {

                } else {

                }
                "Victory!"
            }

            enemyWon -> {
                if (oneWinner) {

                } else {

                }
                "Defeat"
            }

            draw -> {
                "Draw"
            }

            else -> "Game Over"
        }

        guiStage.addActor(GameOverMenu(message, this))
    }

    fun fireEvent(event: Event) {
        if (initView)
            guiStage.root.fire(event)
        playStage.root.fire(event)
    }

    override fun hide() {

    }

    override fun show() {
        if (!isReady) throw IllegalStateException("PlayScreen.isReady is false")
    }

    override fun pause() {

    }

    override fun resume() {

    }

    override fun resize(width: Int, height: Int) {
        playStage.onScreenResize(width, height)
        guiStage.onScreenResize(width, height)
    }

    override fun dispose() {
        guiStage.dispose()
        playStage.dispose()

        backImage.sprite.texture.dispose()

        batch.dispose()

        ambienceMusic.dispose()
        AudioManager.disposeMusic()
    }

    private var gameState: GameStateSnapshot? = null

    private inner class EndConditionListener : EventListener {
        override fun handle(event: Event): Boolean {
            if (event is NextTurnEvent && event.previousPlayer != event.nextPlayer) {
                /** see [ready] fireEvent line */
                checkGameEndCondition()
            }

            return false
        }
    }

    private inner class TouchEventsBlockOnActionListener : EventListener {
        override fun handle(event: Event): Boolean {
            if (event is InputEvent && (event.type == InputEvent.Type.touchDown || event.type == InputEvent.Type.touchUp)) {
                if (actionManager.hasActions) event.stop()

                return true
            }

            return false
        }
    }

    private inner class CommandManagerOnCommandHandler {
        fun onCommand(command: Command) {
            if (isGameOver)
                return

            gameStateId++

            // checkGameEndCondition()
        }
    }

    @Suppress("unused")
    inner class PlayCslCommandExc : CommandExecutor() {
        fun autoEndTurn() {
            GamePref.autoEndTurn = !(GamePref.autoEndTurn ?: false)
        }

        fun win() {
            gameOverSuccess()
        }

        fun lose() {
            gameOverFailure()
        }

        fun addPlayer() {
            addPlayer(1)
        }

        fun addPlayer(id: Int) {
            addPlayer(id, id)
        }

        fun addPlayer(id: Int, teamId: Int) {
            if (turnManager.getPlayerById(id) != null) {
                console.log("Player with such id already exists")
                return
            }

            turnManager.players.add(Player("Player$id", id, teamId))
        }

        fun addAI() {
            addAI(1)
        }

        fun addAI(playerId: Int) {
            val player = turnManager.getPlayerById(playerId)

            if (player == null) {
                console.log("No player with such Id.", LogLevel.ERROR)

                return
            }

            if (botManager.isBotPlayer(player)) {
                console.log("Player is already AI.")

                return
            }

            botManager.add(EasyDuelBot(player, this@PlayScreen))

            GameConsole.console.log("Added AI for player $player")
        }

        @ConsoleDoc(description = "Removes AI for given player Id")
        fun removeAI(playerId: Int) {
            if (!botManager.isBotPlayer(turnManager.getPlayerById(playerId) ?: let {
                    console.log("Player is not found", LogLevel.ERROR)
                    return
                })) {

                console.log("Player is not AI.")
                return
            }

            botManager.botPlayers.removeAll { it.playerID == playerId }
        }

        fun endTurn() {
            queueCommand(EndTurnCommand(turnManager.currentPlayer))
        }

        fun fow() {
            fogOfWarManager.drawFog = !fogOfWarManager.drawFog

            if (fogOfWarManager.drawFog)
                GameConsole.log("Fog is ON.")
            else
                GameConsole.log("Fog is OFF.")
        }

        fun empower() {
            for (unit in playStage.getUnitsOfPlayer(localPlayer)) {
                val abilities = unit.cAbilities?.abilities ?: continue
                unit.mana = 99
                unit.actionPoints = 99
                for (ability in abilities) {
                    ability.currentCooldown = 0
                }
            }
        }

        fun endAction() {
            if (!actionManager.isQueueEmpty) {
                actionManager.getNextInQueue()!!.isDone = true
                GameConsole.console.log("set actionQueue.first().isDone = true")
            } else {
                GameConsole.console.log("actionQueue is empty")
            }
        }

        fun stompQueue() {
            GameConsole.console.log("set stompQueue = ${!commandManager.stop}")
            commandManager.stop = !commandManager.stop
        }

        fun saveState() {
            gameState = createSnapshot()
        }

        fun loadState() {
            if (gameState != null) {
                val newPS = applyGameStateToPlayScreen(gameState!!)
                newPS.localPlayer = newPS.turnManager.getPlayerById(this@PlayScreen.localPlayer.id)
                    ?: let {
                        GameConsole.console.log("Local Player not found", LogLevel.ERROR)
                        return
                    }

                newPS.levelId = this@PlayScreen.levelId
                newPS.ready()
                game.screen = newPS
            } else {
                GameConsole.console.log("Save state first!", LogLevel.ERROR)
            }
        }

        fun reduceAllDamage() {
            for (unit in playStage.getUnits()) {
                unit.cAttack!!.maxDamage -= 5
                unit.cAttack!!.minDamage -= 5
            }
        }

        fun completeTasks() {
            taskManager.completeTasks()
        }

        @ConsoleDoc(description = "Sets the amount of dummies you then can place by pressing 'T' key.")
        fun placeDummies(amount: Int) {
            debugKeysProcessor.dummiesLeft = amount

            GameConsole.console.log("Press 'T' to place a dummy! $amount left.")
        }

        fun debugMode() {
            game.debugMode = !game.debugMode

            GameConsole.console.log(
                if (game.debugMode)
                    "Debug mode on."
                else
                    "Debug mode off."
            )

        }

        @ConsoleDoc(description = "Sets time of day for PlayStage. Params: day, night, evening.")
        fun time(tod: String) {
            when (tod) {
                "day" -> {
                    playStage.timeOfDay = PlayStage.DayTime.DAY
                }

                "night" -> {
                    playStage.timeOfDay = PlayStage.DayTime.NIGHT
                }

                "evening" -> {
                    playStage.timeOfDay = PlayStage.DayTime.EVENING
                }
            }
        }

        @ConsoleDoc(description = "Gives gold to local player.")
        fun gold(amount: Int) {
            localPlayer.credits += amount
        }

        @ConsoleDoc(description = "Gives 9999 gold to local player.")
        fun gold() {
            localPlayer.credits += 9999
        }

        @ConsoleDoc(description = "Gives gold to player with given Id.")
        fun gold(playerId: Int, amount: Int) {
            val player = turnManager.getPlayerById(playerId) ?: let {
                GameConsole.console.log("No player with such Id.", LogLevel.ERROR)
                return
            }

            player.credits += amount
        }

        @ConsoleDoc(description = "Unlocks all units.")
        fun ulckAll() {
            guiStage.buyMenu.unlockAll = true
        }
    }

    private inner class DebugKeysProcessor : InputAdapter() {

        var dummiesLeft = 0

        val playStageCursor
            get() = playStage.screenToStageCoordinates(
                Vector2(
                    Gdx.input.x.toFloat(),
                    Gdx.input.y.toFloat()
                )
            )
        val cursorTiled get() = TilePosition(playStageCursor.x.tiled(), playStageCursor.y.tiled())
        val unitWithinCursor get() = playStage.getUnit(cursorTiled.x, cursorTiled.y)


        override fun keyDown(keycode: Int): Boolean {
            if (!game.debugMode) return false

            when (keycode) {
                Input.Keys.T -> {
                    if (dummiesLeft > 0) {
                        val pos =
                            playStage.screenToStageCoordinates(
                                Vector2(
                                    Gdx.input.x.toFloat(),
                                    Gdx.input.y.toFloat()
                                )
                            )
                        Dummy().addToStage(playStage).position(pos.x.tiled(), pos.y.tiled())

                        dummiesLeft--
                    }
                }

                else -> {
                    if (this@PlayScreen::guiStage.isInitialized) {
                        when (keycode) {
                            Input.Keys.H -> {
                                captureBase()
                            }

                            Input.Keys.X -> {
                                grantXp()
                            }

                            Input.Keys.K -> {
                                kill()
                            }

                            Input.Keys.O -> {
                                move()
                            }
                        }
                    }
                }
            }

            return false
        }

        fun captureBase() {
            val tile = playStage.getTile(
                cursorTiled.x,
                cursorTiled.y
            )
            if (tile?.terrain == TerrainNames.base)
                playStage.getUnitsOfPlayer(localPlayer).random().captureBase(tile)
        }

        fun grantXp() {
            val focusedUnit = unitWithinCursor
            if (focusedUnit?.cLevel != null) {
                if (commandManager.queueCommand(GrantXpCommand(focusedUnit, 250)))
                    guiStage.addActor(FloatingUpLabel("Grant ${focusedUnit.name} 250 xp"))
                else guiStage.addActor(FloatingUpLabel("Unable to give xp to ${focusedUnit.name}"))
            }
        }

        fun kill() {
            val focusedUnit = unitWithinCursor
            if (focusedUnit != null) {
                guiStage.addActor(FloatingUpLabel("Kill ${focusedUnit.name}."))
                focusedUnit.killedBy(focusedUnit, this@PlayScreen)
            }
        }

        fun move() {
            val selectedUnit = guiStage.selectedUnit
            if (selectedUnit != null) {
                guiStage.addActor(FloatingUpLabel("Move ${selectedUnit.name}."))
                selectedUnit.setPositionOrNear(
                    cursorTiled.x,
                    cursorTiled.y
                )
            }
        }
    }
}
