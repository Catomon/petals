package ctmn.petals.playscreen

import com.badlogic.gdx.*
import com.badlogic.gdx.graphics.Color
import com.badlogic.gdx.graphics.GL20
import com.badlogic.gdx.graphics.OrthographicCamera
import com.badlogic.gdx.graphics.Texture
import com.badlogic.gdx.graphics.g2d.Sprite
import com.badlogic.gdx.graphics.g2d.SpriteBatch
import com.badlogic.gdx.math.MathUtils
import com.badlogic.gdx.math.Vector2
import com.badlogic.gdx.scenes.scene2d.Action
import com.badlogic.gdx.scenes.scene2d.Event
import com.badlogic.gdx.scenes.scene2d.EventListener
import com.badlogic.gdx.scenes.scene2d.InputEvent
import com.badlogic.gdx.scenes.scene2d.actions.Actions
import com.strongjoshua.console.CommandExecutor
import com.strongjoshua.console.LogLevel
import com.strongjoshua.console.annotation.ConsoleDoc
import ctmn.petals.Const
import ctmn.petals.screens.MenuScreen
import ctmn.petals.PetalsGame
import ctmn.petals.Rich
import ctmn.petals.ai.AIManager
import ctmn.petals.ai.EasyAiDuelBot
import ctmn.petals.discordRich
import ctmn.petals.effects.FloatingUpLabel
import ctmn.petals.map.*
import ctmn.petals.multiplayer.ClientPlayScreen
import ctmn.petals.multiplayer.HostPlayScreen
import ctmn.petals.multiplayer.json.GameStateSnapshot
import ctmn.petals.multiplayer.applyGameStateToPlayScreen
import ctmn.petals.multiplayer.createSnapshot
import ctmn.petals.player.Player
import ctmn.petals.playscreen.commands.Command
import ctmn.petals.playscreen.commands.CommandManager
import ctmn.petals.playscreen.commands.EndTurnCommand
import ctmn.petals.playscreen.commands.GrantXpCommand
import ctmn.petals.playscreen.events.BaseCapturedEvent
import ctmn.petals.playscreen.events.GameOverEvent
import ctmn.petals.playscreen.events.NextTurnEvent
import ctmn.petals.playscreen.gui.PlayGUIStage
import ctmn.petals.playscreen.gui.PlayStageCameraController
import ctmn.petals.playscreen.gui.widgets.FogOfWarDrawer
import ctmn.petals.playscreen.gui.widgets.MarkersDrawer
import ctmn.petals.playscreen.listeners.PinkSlimeLingHealing
import ctmn.petals.playscreen.listeners.TileLifeTimeListener
import ctmn.petals.playscreen.listeners.TurnsCycleListener
import ctmn.petals.playscreen.seqactions.SeqActionManager
import ctmn.petals.playscreen.tasks.TaskManager
import ctmn.petals.playscreen.triggers.TriggerManager
import ctmn.petals.playstage.*
import ctmn.petals.pvp.newPvPAlice
import ctmn.petals.story.gameOverFailure
import ctmn.petals.story.gameOverSuccess
import ctmn.petals.tile.*
import ctmn.petals.tile.components.CapturingComponent
import ctmn.petals.unit.*
import ctmn.petals.unit.actors.Dummy
import ctmn.petals.utils.*
import ctmn.petals.widgets.newLabel
import kotlin.random.Random

open class PlayScreen(
    val game: PetalsGame = ctmn.petals.game,
) : Screen {

    /** Just telling if we ever gonna init [guiStage] */
    var initView = true

    val batch = SpriteBatch()
    val assets = game.assets

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
    val aiManager = AIManager(this)

    val fogOfWarManager = FogOfWarDrawer(this)

    val playStageCameraController = PlayStageCameraController(playStage)

    var friendlyFire = false

    var creditsPerBase = 100 //TODO GameState
    var creditsPerCluster = 75

    var gameType = GameType.STORY
    var gameMode = GameMode.ALL
    var gameEndCondition: GameEndCondition = NoEnd()
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

    init {
        discordRich(Rich.PLAYING)

        playStageSetup()

        commandManager.onCommand = onCommand@{ CommandManagerOnCommandHandler().onCommand(it) }
    }

    private fun playStageSetup() {
        playStage.camera.position.x = Gdx.graphics.width / 2f
        playStage.camera.position.y = Gdx.graphics.height / 2f

        playStage.addActorAfterTiles(fogOfWarManager)
        playStage.root.addActorAfter(fogOfWarManager, MarkersDrawer())

        // stop touch events if an action is processing
        playStage.addCaptureListener(TouchEventsBlockOnActionListener())

        // game logic
        playStage.addListener(PlayTurnCycleListener())
        playStage.addListener(EndConditionListener())

        // less important game logic
        playStage.addListener(TileLifeTimeListener(playStage))
        playStage.addListener(PinkSlimeLingHealing())
    }

    fun update(delta: Float) {
        playStageCameraController.update(delta)

        actionManager.update(delta)
        aiManager.update(delta)
        commandManager.update(delta)
        taskManager.update(delta)
        triggerManager.update(delta)
    }

    fun setLevel(map: MapConverted) {
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

        // if first tile on a tiled position has layer != 1, shift all tiles layer on the position to make it 1
        // like 0 1 2 -> -1 0 1
        val tiles = playStage.getAllTiles().toMutableList()
        val tilesSamePos = mutableListOf<TileActor>()
        for (x in 0 until playStage.tiledWidth) {
            for (y in 0 until playStage.tiledHeight) {
                tilesSamePos.clear()
                tiles.forEach { if (it.tiledX == x && it.tiledY == y) tilesSamePos.add(it) }
                tilesSamePos.sortByDescending { it.layer }
                if (tilesSamePos.isEmpty()) continue

                if (tilesSamePos.first().layer == 1) continue

                if (tilesSamePos.size == 1) {
                    tilesSamePos.first().layer = 1
                    continue
                }

                val layerDownTo = 1 - tilesSamePos.first().layer
                tilesSamePos.forEach { it.layer += layerDownTo }
            }
        }

        playStage.clearTiles()
        tiles.forEach { playStage.addActor(it) }

        levelCreated()
    }

    fun levelCreated() {
        // make border
        playStage.border.make()

        // decorate
        Decorator(this, playStage).decorate()

        fogOfWarManager.updateGridMap()
    }

    private var isReady = false

    open fun ready() {
        if (levelId == null) throw IllegalStateException("Level is null")

        when (gameMode) {
            GameMode.CRYSTALS -> {
                for (label in playStage.getLabels()) {
                    if (label.labelName == "player") {
                        val playerId = label.data["player_id"] ?: continue
                        val player = turnManager.players.firstOrNull { it.id == playerId.toInt() } ?: continue

                        // label.data["leader_unit_name"]
                        newPvPAlice.addToStage(playStage).player(player).position(label)
                            .leader(MathUtils.random(0, 9999999))
                    }
                }
            }

            GameMode.CASTLES -> {
                var playerCount = 0
                for (base in playStage.getTiles().filter { it.name.contains("base") && it.cPlayerId != null }) {

                    if (turnManager.players.size == playerCount) break

                    base.cPlayerId!!.playerId = turnManager.players[playerCount].id

                    playerCount++
                }

                for (i in playerCount until turnManager.players.size) {
                    turnManager.players[i].isOutOfGame = true
                }
            }

            else -> {}
        }

        if (!::guiStage.isInitialized) initGui()

        isReady = true

        // give first turn player gold for all bases
        val currentPlayer = turnManager.currentPlayer
        for (base in playStage.getCapturablesOf(currentPlayer))
            currentPlayer.gold += Const.GOLD_PER_BASE


        fireEvent(NextTurnEvent(turnManager.currentPlayer, turnManager.currentPlayer))
    }

    /** Should be called after TileData, Units, Players, and CurrentPlayer were added to playStage */
    open fun initGui() {
        inputMultiplexer.processors.clear()

        guiStage = PlayGUIStage(this)

        inputMultiplexer.addProcessor(guiStage)
        inputMultiplexer.addProcessor(playStage)
        inputMultiplexer.addProcessor(debugKeysProcessor)

        Gdx.input.inputProcessor = inputMultiplexer
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

    private fun checkGameEndCondition() {
        if (gameEndCondition.check(this)) {
            isGameOver = true

            queueAction {
                val labelGameOver = newLabel("GameOver", "font_8").also { it.isVisible = false }

                when (gameEndCondition) {
                    is AnyTeamStand -> {
                        if ((gameEndCondition as AnyTeamStand).result != GameEndCondition.Result.DRAW)
                            labelGameOver.setText(
                                "${
                                    turnManager.getPlayerById(
                                        playStage.getUnitsForTeam((gameEndCondition as AnyTeamStand).teamStandId)
                                            .first().playerId
                                    )?.name
                                } Won"
                            )
                        else
                            labelGameOver.setText("Draw")
                    }

                    is TeamStand -> labelGameOver.setText("You Won")
                    is AnyTeamBaseStand -> {
                        if ((gameEndCondition as AnyTeamBaseStand).result != GameEndCondition.Result.DRAW)
                            labelGameOver.setText(
                                "${
                                    turnManager.getPlayerById(
                                        playStage.getUnitsForTeam((gameEndCondition as AnyTeamBaseStand).teamStandId)
                                            .first().playerId
                                    )?.name
                                } Won"
                            )
                        else
                            labelGameOver.setText("Draw")
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
                        onGameOver()
                        return true
                    }
                }))
            }
        }
    }

    open fun onGameOver() {
        fireEvent(GameOverEvent())

        //game.screen = MenuScreen(game)
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
        playStage.viewport.update(width, height, false)
        guiStage.onScreenResize(width, height)
    }

    override fun dispose() {
        guiStage.dispose()
        playStage.dispose()

        backImage.sprite.texture.dispose()

        batch.dispose()
    }

    private var gameState: GameStateSnapshot? = null

    private inner class PlayTurnCycleListener : EventListener {
        override fun handle(event: Event): Boolean {
            val turnCycleEvent = if (event is TurnsCycleListener.TurnCycleEvent) event else return false

            // give next player gold for every base
            val nextPlayer = turnCycleEvent.nextPlayer
            if (!nextPlayer.isOutOfGame) {
                for (base in playStage.getCapturablesOf(nextPlayer)) {
                    if (base.isBase)
                        nextPlayer.gold += creditsPerBase
                    else
                        nextPlayer.gold += creditsPerCluster
                }
            }

            //put ability on cooldown if it was partially cast
            for (unit in playStage.getUnits()) {
                val abilities = unit.cAbilities?.abilities
                if (abilities != null)
                    for (ability in abilities)
                        if (ability.castAmountsLeft < ability.castAmounts) {
                            ability.castAmountsLeft = ability.castAmounts
                            ability.currentCooldown = ability.cooldown
                        }
            }

            for (unit in playStage.getUnits()) {
                //update buffs duration
                val iterator = unit.buffs.iterator()
                while (iterator.hasNext()) {
                    val buff = iterator.next()
                    buff.duration -= turnCycleEvent.turnCycleTime

                    if (buff.duration <= 0) {
                        iterator.remove()
                        unit.updateView()
                    }
                }

                if (unit.playerId == turnCycleEvent.nextPlayer.id) {
                    //reset units ap
                    if (unit.buffs.find { buff -> buff.name == "freeze" } == null)
                        unit.actionPoints = Const.ACTION_POINTS

                    //update abilities cooldowns
                    if (unit.cAbilities != null)
                        for (ability in unit.cAbilities!!.abilities) {
                            if (ability.currentCooldown > 0)
                                ability.currentCooldown -= 1
                            else
                                ability.castAmountsLeft = ability.castAmounts
                        }

                    //add mana for crystals
                    if (gameMode == GameMode.CRYSTALS) {
                        if (unit.isLeader && unit.cAbilities != null) {
                            for (tile in playStage.getTiles()) {
                                if (tile.terrain == "base"
                                    && tile.name.contains("crystal")
                                    && tile.cPlayerId?.playerId == unit.playerId
                                ) {
                                    unit.mana += 5
                                }
                            }
                        }
                    }
                }
            }

            //update bases capturing
            for (tile in playStage.getTiles()) {
                if (tile.isCapturable &&
                    tile.cCapturing?.playerId == turnCycleEvent.nextPlayer.id
                ) {
                    val unitCaptures = playStage.getUnit(tile.tiledX, tile.tiledY)
                    (if (tile.cCapturing!!.playerId == unitCaptures?.playerId)
                        unitCaptures
                    else null
                            )?.captureBase(tile).also {
                            playStage.root.fire(BaseCapturedEvent(tile))
                        } ?: tile.components.remove(CapturingComponent::class.java)
                }
            }

            //summoner component
            for (unit in playStage.getUnitsOfPlayer(turnCycleEvent.lastPlayer)) {
                unit.cSummoner?.giveAP = false
            }

            //heal units that are near their leader
            for (unit in playStage.getUnitsOfPlayer(turnCycleEvent.nextPlayer)) {
                if (unit.isUnitNear(playStage.getLeadUnit(unit.followerID) ?: continue, 1))
                    if (unit.health < unit.unitComponent.baseHealth)
                        unit.heal(Const.HEALING_AMOUNT_NEAR_LEADER)
            }



            return false
        }
    }

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

            // Mark player out of game if they have no units or non-occupied bases
            for (player in turnManager.players) {
                player.isOutOfGame =
                    player.isOutOfGame || gameEndCondition.checkPlayerOutOfGame(player, this@PlayScreen) == true
            }

            checkGameEndCondition()
        }
    }

    @Suppress("unused")
    inner class PlayCslCommandExc : CommandExecutor() {
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

            if (aiManager.isAIPlayer(player)) {
                console.log("Player is already AI.")

                return
            }

            aiManager.add(EasyAiDuelBot(player, this@PlayScreen))

            GameConsole.console.log("Added AI for player $player")
        }

        @ConsoleDoc(description = "Removes AI for given player Id")
        fun removeAI(playerId: Int) {
            if (!aiManager.isAIPlayer(turnManager.getPlayerById(playerId) ?: let {
                    console.log("Player is not found", LogLevel.ERROR)
                    return
                })) {

                console.log("Player is not AI.")
                return
            }

            aiManager.aiPlayers.removeAll { it.playerID == playerId }
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
            localPlayer.gold += amount
        }

        @ConsoleDoc(description = "Gives 9999 gold to local player.")
        fun gold() {
            localPlayer.gold += 9999
        }

        @ConsoleDoc(description = "Gives gold to player with given Id.")
        fun gold(playerId: Int, amount: Int) {
            val player = turnManager.getPlayerById(playerId) ?: let {
                GameConsole.console.log("No player with such Id.", LogLevel.ERROR)
                return
            }

            player.gold += amount
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
                            playStage.screenToStageCoordinates(Vector2(Gdx.input.x.toFloat(), Gdx.input.y.toFloat()))
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
            if (tile?.terrain == Terrain.base)
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
