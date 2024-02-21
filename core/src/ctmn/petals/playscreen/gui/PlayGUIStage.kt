package ctmn.petals.playscreen.gui

import ctmn.petals.*
import ctmn.petals.Const.PLAY_GUI_VIEWPORT_HEIGHT
import ctmn.petals.Const.PLAY_GUI_VIEWPORT_WIDTH
import ctmn.petals.Const.TILE_SIZE
import ctmn.petals.player.Player
import ctmn.petals.playscreen.*
import ctmn.petals.playscreen.seqactions.CameraMoveAction
import ctmn.petals.playscreen.commands.*
import ctmn.petals.playscreen.events.*
import ctmn.petals.playscreen.gui.widgets.*
import ctmn.petals.playscreen.tasks.DialogTask
import ctmn.petals.story.aliceOrNull
import ctmn.petals.unit.*
import ctmn.petals.unit.abilities.SummonAbility
import ctmn.petals.utils.*
import ctmn.petals.widgets.newImageButton
import com.badlogic.gdx.Gdx
import com.badlogic.gdx.Input
import com.badlogic.gdx.graphics.Color
import com.badlogic.gdx.graphics.OrthographicCamera
import com.badlogic.gdx.scenes.scene2d.*
import com.badlogic.gdx.scenes.scene2d.utils.ChangeListener
import com.badlogic.gdx.scenes.scene2d.utils.ClickListener
import com.badlogic.gdx.utils.viewport.ExtendViewport
import com.kotcrab.vis.ui.VisUI
import com.kotcrab.vis.ui.widget.*
import ctmn.petals.multiplayer.ClientPlayScreen
import ctmn.petals.playstage.*
import ctmn.petals.tile.*
import ctmn.petals.unit.UnitActor
import ctmn.petals.widgets.addChangeListener
import ctmn.petals.widgets.newLabel


class PlayGUIStage(
    val playScreen: PlayScreen,
) : Stage(ExtendViewport(PLAY_GUI_VIEWPORT_WIDTH, PLAY_GUI_VIEWPORT_HEIGHT), playScreen.batch) {
    /** see [onScreenResize] */

    val assets = playScreen.assets

    //localPlayer
    val player: Player get() = playScreen.localPlayer

    //stage
    val playStage = playScreen.playStage
    val playStageCamera = playScreen.playStage.camera as OrthographicCamera

    /// Widgets
    //tables
    private val dialogTable = VisTable()
    val turnIconTable = VisTable()
    val abilityChooseDialogTable = VisTable()
    val buyMenuPanelTable = VisTable()

    //panels and windows and stuff
    private val unitPanel = UnitPanel(this)

    val tasksTable = TasksTable()
    private val turnIcon = TurnIcon()

    //labels
    val creditsLabel =
        newLabel("Credits: ${player.gold}", "font_5").apply { isVisible = playScreen.gameMode == GameMode.CASTLES }
    private val fpsLabel = VisLabel()

    //buttons
    private val zoomButton = newImageButton("zoom").apply { isVisible = false }
    private val infoButton = newImageButton("info").apply { isVisible = false }
    private val pauseButton = newImageButton("pause")
    val endTurnButton = newImageButton("end_turn")
    private val backButtonStyle = VisUI.getSkin().get("back", VisImageButton.VisImageButtonStyle::class.java)
    private val cancelButtonStyle =
        VisUI.getSkin().get("cancel_in-game", VisImageButton.VisImageButtonStyle::class.java)
    private val cancelButton: VisImageButton = newImageButton("back").apply { isVisible = false }
    private val refreshButton = newImageButton("refresh")
    val abilitiesPanel = AbilitiesPanel(this)
    private val unitMiniMenu = UnitMiniMenu(this)
    val nextDialogButton = StoryDialog.NextDialogButton(this)
    private val captureButton = newImageButton("capture").apply { isVisible = false }

    //not widgets
    val tileSelectionDrawer = TileSelectionDrawer(this)
    private val attackIconsDrawer = AttackIconsDrawer(this).also { it.isVisible = true }
    private val leaderFieldDrawer = LeaderFieldDrawer(this)
    private val unitInfoDrawer = UnitInfoDrawer(this)
    private val animationsUpdater = AnimationsUpdater(this)
    private val movementCostDrawer = MovementCostDrawer(this)
    val tileHighlighter = TileHighlighter(this)

    //ability and movement borders (playStage)
    private val movementBorder = AttackMovementRangeDrawer(this)
    val abilityRangeBorder = BorderDrawer(Color.VIOLET, this)
    val abilityActivationRangeBorder = BorderDrawer(Color.VIOLET, this)

    //other
    private val mapFrame = VisImage(VisUI.getSkin().getPatch("map_frame"))

    //state
    val myTurn = State()
    val endTurn = State()
    val startTurn = State()
    val theirTurn = State()
    val nextPlayerPrepare = State()
    val gameOverState = State()
    private val loseState = State()
    private val winState = State()
    var currentState: State = State()
        set(value) {
            field.onExit?.invoke()
            value.onEnter?.invoke()
            field = value
        }

    private val holdStateTimer = Timer(0f)

    //click strategy
    val blockedCs = BlockedCs()
    val seeInfoCs = SeeInfoCs()
    val selectUnitCs = SelectUnitCs()
    val unitSelectedCs = UnitSelectedCs()
    val useAbilityCs = UseAbilityCs()
    val confirmAbilityCs = ConfirmAbilityCs()

    //val summonDollCs = SummonDollCs()
    var clickStrategy: ClickStrategy = selectUnitCs
        set(value) {

            //hide borders on exit
            when {
                field is UnitSelectedCs && value !is UnitSelectedCs -> {
                    // fire event
                }

                field is UseAbilityCs && value !is UseAbilityCs -> {
                    abilityRangeBorder.isVisible = false

                    tileHighlighter.clearHighlights()
                    (abilitiesPanel.cells.firstOrNull { it.actor is SummonAbilityButton }?.actor as SummonAbilityButton?)?.hidePane()
                }

                field is ConfirmAbilityCs && value !is ConfirmAbilityCs -> {
                    abilityActivationRangeBorder.isVisible = false
                }
            }

            when (value) {
                is UnitSelectedCs -> {

                }

                is UseAbilityCs -> {
                    if (selectedUnit == null) throw IllegalStateException("While clickStrategy is UseAbilityCs, selectedUnit shouldn't be null.")

                    abilityRangeBorder.isVisible = true

                    tileHighlighter.clearHighlights()

                    if (abilitiesPanel.selectedAbility is SummonAbility) {
                        tileHighlighter.highlightSummoning(selectedUnit!!, 1)

                        abilityRangeBorder.isVisible = false
                    }
                }

                is ConfirmAbilityCs -> {
                    abilityActivationRangeBorder.isVisible = true
                }
            }

            if (field !is BlockedCs) //why
                field = value
        }

    //use selectUnit(unit)
    var selectedUnit: UnitActor? = null
    val isUnitSelected get() = selectedUnit != null

    var forceMyTurnEnd = false

    val inputManager = InputManager(this)

    init {
        playStage.zoomCameraByDefault()
        playStage.centerCameraByDefault()

//        playStage.addActorAfterTiles(mapFrame)
//        with(mapFrame) {
//            val offset = (width - 16) / 2
//            setPosition(-offset, -offset)
//            width = playStage.mapWidth() + offset * 2
//            height = playStage.mapHeight() + offset * 2
//        }

        //setup actors
        playStage.addActor(attackIconsDrawer)
        playStage.addActor(tileHighlighter)
        playStage.addActor(tileSelectionDrawer)
        playStage.addActor(unitInfoDrawer)
        playStage.addActor(movementCostDrawer)
        playStage.addActorAfterTiles(movementBorder)
        playStage.addActorAfterTiles(abilityRangeBorder)
        playStage.addActorAfterTiles(abilityActivationRangeBorder)
        playStage.addActorAfterTiles(leaderFieldDrawer)

        addActor(animationsUpdater)

        addActor(unitPanel.table)
        addActor(turnIconTable)

        setupTable()
        addActor(dialogTable)

        abilityChooseDialogTable.setFillParent(true)
        addActor(abilityChooseDialogTable)

        buyMenuPanelTable.setFillParent(true)
        addActor(buyMenuPanelTable)

        addActor(unitMiniMenu)

        //add widgets listeners
        zoomButton.addListener(object : ClickListener() {
            override fun clicked(event: InputEvent?, x: Float, y: Float) {
                super.clicked(event, x, y)

                playStageCamera.zoom = when {
                    playStageCamera.zoom == 0.5f -> 0.6f
                    playStageCamera.zoom == 0.6f -> 0.7f
                    playStageCamera.zoom == 0.7f -> 0.8f
                    playStageCamera.zoom == 0.8f -> 0.9f
                    playStageCamera.zoom == 0.9f -> 1f
                    playStageCamera.zoom == 1f -> 1.25f
                    playStageCamera.zoom == 1.25f -> 1.5f
                    playStageCamera.zoom == 1.5f -> {
                        playStage.zoomCameraByDefault(); playStageCamera.zoom
                    }

                    playStageCamera.zoom != 0.5f -> 0.5f
                    else -> {
                        playStage.zoomCameraByDefault(); playStageCamera.zoom
                    }
                }
            }
        })

        cancelButton.addListener(object : ClickListener() {
            override fun clicked(event: InputEvent?, x: Float, y: Float) {
                super.clicked(event, x, y)

                if (isUnitSelected) {
                    selectUnit(null)
                    cancelButton.style = backButtonStyle
                }
            }
        })

        pauseButton.addListener(object : ClickListener() {
            override fun clicked(event: InputEvent, x: Float, y: Float) {
                super.clicked(event, x, y)

                playScreen.returnToMenuScreen()
            }
        })

        if (playScreen.gameType == GameType.MULTIPLAYER) {
            if (playScreen is ClientPlayScreen) {
                refreshButton.addChangeListener {
                    playScreen.requestGameState()
                }
            }
        }

        endTurnButton.addListener(object : ChangeListener() {
            override fun changed(event: ChangeEvent, actor: Actor) {
                for (task in playScreen.taskManager.getTasks()) {
                    if (task.isForcePlayerToComplete)
                        return
                }

                if (playScreen.actionManager.hasActions) return

                playScreen.commandManager.queueCommand(EndTurnCommand(player))
            }
        })

        captureButton.addChangeListener {
            selectedUnit?.let { unit ->
                if (unit.isPlayerUnit(player)) {
                    val tile = playStage.getTile(unit.tiledX, unit.tiledY) ?: return@addChangeListener
                    if (!tile.isCapturable) return@addChangeListener
                    val captureCommand = CaptureCommand(unit.name, tile.name)
                    if (!captureCommand.canExecute(playScreen)) return@addChangeListener

                    playScreen.queueCommand(captureCommand)
                }
            }

            //captureButton.isDisabled = true
            captureButton.isVisible = false
        }

        addListener {
            val unit: UnitActor? = when (it) {
                is UnitSelectedEvent -> it.unit
                is UnitMovedEvent -> it.unit
                else -> return@addListener false
            }

            captureButton.isVisible = false

            unit ?: return@addListener false

            if (unit.isPlayerUnit(player) && unit.actionPoints > 0) {
                val tile = playStage.getTile(unit.tiledX, unit.tiledY) ?: return@addListener false
                if (!tile.isCapturable) return@addListener false

                captureButton.isVisible = true

                if (tile.cPlayerId?.playerId == player.id) {
                    //captureButton.isDisabled = true
                    captureButton.isVisible = false
                    return@addListener false
                }

                captureButton.isDisabled = false
            }

            false
        }

        // States
        // Start turn
        startTurn.update = {
            if (holdStateTimer.isDone)
                currentState = myTurn
        }

        startTurn.onEnter = {
            clickStrategy = seeInfoCs

            holdStateTimer.start(0.25f)

            selectUnit(null)

            // Move camera to the localPlayer's unit leader/first unit on the start of the turn
            val unit = playScreen.aliceOrNull() ?: playStage.getUnitsOfPlayer(playScreen.localPlayer)
                .firstOrNull { unit -> unit.isLeader } ?: playStage.getUnitsOfPlayer(playScreen.localPlayer)
                .firstOrNull()
            if (unit != null) playScreen.actionManager.addAction(CameraMoveAction(unit.centerX, unit.centerY))
        }

        // My turn
        myTurn.onEnter = {
            clickStrategy = selectUnitCs

            endTurnButton.isDisabled = false

            if (forceMyTurnEnd)
                playScreen.commandManager.queueCommand(EndTurnCommand(player))
        }

        myTurn.onExit = {
            endTurnButton.isDisabled = true
        }

        // End turn
        endTurn.update = {
            if (holdStateTimer.isDone) {
                currentState = if (playScreen.gameType == GameType.PVP_SAME_SCREEN) {
                    if (!playScreen.aiManager.isAIPlayer(playScreen.turnManager.currentPlayer)) {
                        nextPlayerPrepare
                    } else
                        theirTurn
                } else
                    theirTurn
            }
        }

        endTurn.onExit = {

        }

        endTurn.onEnter = {
            clickStrategy = seeInfoCs

            holdStateTimer.start(1f)

            selectUnit(null)
        }

        // Their turn
        theirTurn.onEnter = {
            clickStrategy = seeInfoCs
        }

        // Game Over
        gameOverState.onEnter = {
            clickStrategy = blockedCs
        }

        // Lose
        loseState.onEnter = {
            clickStrategy = blockedCs
        }

        // Win
        winState.onEnter = {
            clickStrategy = blockedCs
        }

        // Next Player
        nextPlayerPrepare.onEnter = {
            clickStrategy = seeInfoCs

            playScreen.fogOfWarManager.hideAll = true

            val labelPlayerReady = newLabel("Player Ready", "font_8")
            addActor(labelPlayerReady)
            labelPlayerReady.setPosition(camera.position.x - labelPlayerReady.width / 2, camera.position.y)
            labelPlayerReady.isVisible = true
            labelPlayerReady.addAction(object : Action() {
                override fun act(delta: Float): Boolean {
                    // see also InputManager
                    if (Gdx.input.isKeyJustPressed(Input.Keys.SPACE)
                        || Gdx.input.justTouched()
                    ) {

                        labelPlayerReady.remove()

                        return true
                    }

                    return false
                }
            })
        }

        nextPlayerPrepare.update = {
            if (currentState == nextPlayerPrepare)
                endTurnButton.isDisabled = true
        }

        nextPlayerPrepare.onExit = {
            playScreen.fogOfWarManager.hideAll = false

            endTurnButton.isDisabled = false
        }

        currentState = if (playScreen.turnManager.currentPlayer == player) myTurn else theirTurn
        // States setup ^

        // Listeners
        // disable or enable end turn button
        addListener {
            when (it) {
                is ActionStartedEvent -> {
                    endTurnButton.isDisabled = true
                }

                is ActionCompletedEvent -> {
                    if (!playScreen.actionManager.hasActions && playScreen.turnManager.currentPlayer == player)
                        endTurnButton.isDisabled = false
                }

                is TaskBeginEvent -> {
                    if (it.task.isForcePlayerToComplete || it.task is DialogTask) {
                        endTurnButton.isDisabled = true

                        it.task.addOnCompleteTrigger {
                            for (task in playScreen.taskManager.getTasks())
                                if (task.isForcePlayerToComplete || task is DialogTask)
                                    return@addOnCompleteTrigger

                            if (!playScreen.actionManager.hasActions && playScreen.turnManager.currentPlayer == player)
                                endTurnButton.isDisabled = false
                        }
                    }
                }

                is NextTurnEvent -> {
                    endTurnButton.isDisabled =
                        playScreen.actionManager.hasActions || playScreen.turnManager.currentPlayer != player
                }
            }

            false
        }

        // NextTurn and GameOver Event Listener
        addListener {
            when (it) {
                is GameOverEvent -> {
                    currentState = gameOverState
                }

                is NextTurnEvent -> {
                    when (playScreen.gameType) {
                        GameType.PVP_SAME_SCREEN -> {
                            // see nextPlayerPrepare
                        }

                        else -> {
                            if (playScreen.turnManager.currentPlayer.id == player.id)
                                currentState = startTurn
                        }
                    }
                }
            }

            false
        }

        addListener {
            if (it is TileClickedEvent) {
                //show unit buy menu when base clicked
                val tile = it.tile
                if (tile.isBase) {
                    if (playScreen.turnManager.currentPlayer == player
                        && tile.cPlayerId?.playerId == player.id
                        && currentState == myTurn
                    )
                        buyMenuPanelTable.add(BuyMenuPanel(this, it.tile, player))
                }
            }

            false
        }

        // Play stage listeners
        playStage.addListener {
            when (it) {
                is UnitBoughtEvent -> creditsLabel.setText("Crystals: ${player.gold}")
                is NextTurnEvent -> creditsLabel.setText("Crystals: ${player.gold}")
            }

            false
        }

        //a listener that performs unit commands on an event depending on clickStrategy value
        addListener { event ->
            when (event) {
                is MapClickedEvent -> {
                    clickStrategy.onMapClicked(event.clickX.tiled(), event.clickY.tiled())
                }

                is TileClickedEvent -> {
                    clickStrategy.onTileClicked(event.tile)
                }

                is UnitClickedEvent -> {
                    clickStrategy.onUnitClicked(event.unit)
                }
            }

            false
        }

        //unselect dead unit
        playStage.addListener {
            if (it is UnitDiedEvent && selectedUnit == it.unit) {
                selectUnit(null)
            }

            false
        }

        // unit level up listener
        playStage.addListener {
            if (it is UnitLevelUpEvent && it.unit.playerId == player.id) {
                abilityChooseDialogTable.add(LevelUpWindow(it.unit)).center() //AbilitiesSelectTable(it.unit)
            }

            return@addListener false
        }

        //select AI unit in action
        playStage.addListener { event ->
            if (GamePref.showAiGui != true) return@addListener false

            if (playScreen.aiManager.isAIPlayer(playScreen.turnManager.currentPlayer)) {
                when (event) {
                    is CommandAddedEvent -> {
                        val unit = when (val command = event.command) {
                            is MoveUnitCommand -> playStage.findUnit(command.unitId)
                            is AttackCommand -> playStage.findUnit(command.attackerUnitId)
                            is CaptureCommand -> playStage.findUnit(command.unitId)
                            else -> null
                        }
                        if (unit != null && unit.isPlayerUnit(playScreen.turnManager.currentPlayer)) {
                            selectUnit(unit)
                        }
                    }

                    is UnitBoughtEvent -> {
                        val unit = event.unit
                        if (unit.isPlayerUnit(playScreen.turnManager.currentPlayer)) {
                            selectUnit(unit)
                        }
                    }
                }
            }
            false
        }
    }

    private fun setupTable() {
        dialogTable.setFillParent(true)
        turnIconTable.setFillParent(true)

        val topRightButtonsTable = VisTable()
        with(topRightButtonsTable) {
            add(pauseButton).right().top()
            row()
            add(infoButton).right().top()
            row()
            add(zoomButton).right().top()
            row()
            add(cancelButton).right().top()
            row()
            if (playScreen.gameType == GameType.MULTIPLAYER) {
                if (playScreen is ClientPlayScreen) {
                    add(refreshButton).right().top()
                    row()
                }
            }
        }

        with(createTable()) {
            top().right().padRight(1f * 3f).padTop(1f * 3f)
            add(fpsLabel).top().right().height(10f * 3f).padRight(20f)
            add(creditsLabel).top().right().height(10f * 3f).padRight(1f * 3f)
            add(topRightButtonsTable).top().right()
        }

        with(createTable()) {
            bottom()
            add(captureButton)
            row()
            add(abilitiesPanel)
        }
        createTable(endTurnButton).bottom().right()

        with(turnIconTable) {
            top()
            add(turnIcon).top().center()
            row()
            add(turnIcon.label).top().center()
        }

        addActor(VisTable().apply {
            setFillParent(true)
            top()
            left()
            add(tasksTable).left().top()
        })
    }

    override fun act(delta: Float) {
        super.act(delta)

        holdStateTimer.update(delta)

        fpsLabel.setText("FPS:${Gdx.graphics.framesPerSecond}")
    }

    override fun draw() {
        currentState.update?.invoke()

//        batch.begin()
//        val batch = batch as SpriteBatch
//
//        batch.end()

        super.draw()
    }

    fun reset() {
        currentState = when (player) {
            playScreen.turnManager.currentPlayer -> myTurn
            else -> theirTurn
        }
        selectUnit(null)
    }

    fun selectUnit(unit: UnitActor?) {
        selectedUnit =
            if (unit != null && (unit.stage == null || !unit.isAlive()))
                null
            else
                unit

        root.fire(UnitSelectedEvent(selectedUnit))

        if (selectedUnit == null)
            cancelButton.style = backButtonStyle
        else
            cancelButton.style = cancelButtonStyle

        if (selectedUnit == null) {
            clickStrategy = if (myTurn.isCurrent()) selectUnitCs else seeInfoCs

            abilitiesPanel.setUpForUnit(null)
            //unitMiniMenu.unit = null
            return
        }

        if (!selectedUnit!!.isPlayerUnit(player) || !myTurn.isCurrent()) {
            abilitiesPanel.setUpForUnit(null)

            clickStrategy = seeInfoCs
            return
        }

        clickStrategy = unitSelectedCs

        if (selectedUnit!!.isPlayerUnit(player)) {
            //if (selectedUnit!!.isLeader || selectedUnit!!.isFollower) {
            val leader =
                if (selectedUnit!!.isFollower) playStage.getLeadUnit(selectedUnit!!.followerID) else selectedUnit!!
            //setup abilities panel for selected unit or it's leader
            if (leader != null && selectedUnit!!.cAbilities == null)
                abilitiesPanel.setUpForUnit(leader)
            else abilitiesPanel.setUpForUnit(selectedUnit!!)
            //}
            //unitMiniMenu.unit = selectedUnit
        }
    }

    /** Fires @MapClickedEvent and, if clickStrategy has not changed, UnitClickedEvent TileClickedEvent */
    fun onMapClicked(clickX: Float, clickY: Float) {
        val tiledX: Int = clickX.toInt() / TILE_SIZE
        val tiledY: Int = clickY.toInt() / TILE_SIZE

        var clickedUnit: UnitActor? = null
        var clickedTile: TileActor? = null
        for (actor in playStage.getTilesAndUnits()) {
            if (actor is UnitActor) {
                if (actor.tiledX == tiledX && actor.tiledY == tiledY && actor.isVisible)
                    clickedUnit = actor
            }
            if (actor is TileActor) {
                if (actor.tiledX == tiledX && actor.tiledY == tiledY)
                    clickedTile = actor
            }
        }

        val oldClickStrategy = clickStrategy

        root.fire(MapClickedEvent(clickX, clickY))

        if (oldClickStrategy == clickStrategy) {
            if (clickedUnit != null)
                root.fire(UnitClickedEvent(clickedUnit))
            else
                if (clickedTile != null)
                    root.fire(TileClickedEvent(clickedTile))
        }
    }

    fun State.isCurrent(): Boolean {
        return this == currentState
    }

    fun onScreenResize(width: Int, height: Int) {
        (viewport as ExtendViewport).minWorldWidth = width / Const.GUI_SCALE
        (viewport as ExtendViewport).minWorldHeight = height / Const.GUI_SCALE

        viewport.update(width, height)

        camera.resetPosition()

        dialogTable.setPosition(
            viewport.camera.position.x - viewport.camera.viewportWidth / 2,
            viewport.camera.position.y - viewport.camera.viewportHeight / 2
        )
        turnIconTable.setPosition(
            viewport.camera.position.x - viewport.camera.viewportWidth / 2,
            viewport.camera.position.y - viewport.camera.viewportHeight / 2
        )
    }

    /** What da click doin */
    interface ClickStrategy {
        fun onMapClicked(tiledX: Int, tiledY: Int): Boolean = false
        fun onTileClicked(tile: TileActor): Boolean = false
        fun onUnitClicked(unit: UnitActor): Boolean = false
    }

    inner class BlockedCs : ClickStrategy

    inner class SeeInfoCs : ClickStrategy {
        override fun onTileClicked(tile: TileActor): Boolean {
            selectUnit(null)

            return false
        }

        override fun onUnitClicked(unit: UnitActor): Boolean {
            selectUnit(unit)

            return false
        }
    }

    inner class SelectUnitCs : ClickStrategy {
        override fun onUnitClicked(unit: UnitActor): Boolean {
            if (myTurn.isCurrent()) {
                selectUnit(unit)
                return true
            }

            return false
        }
    }

    inner class UnitSelectedCs : ClickStrategy {
        override fun onTileClicked(tile: TileActor): Boolean {
            if (myTurn.isCurrent()) {
                if (selectedUnit!!.canMove()) {
                    val command = MoveUnitCommand(selectedUnit!!, tile.tiledX, tile.tiledY)
                    if (command.canExecute(playScreen) && playScreen.commandManager.queueCommand(command)) {
                        selectUnit(selectedUnit)
                        return true
                    } else selectUnit(null) //if unit can't move there
                } else selectUnit(null) //if unit can't move
            }

            return false
        }

        override fun onUnitClicked(unit: UnitActor): Boolean {
            if (myTurn.isCurrent()) {
                if (selectedUnit == unit) {
                    selectUnit(null)
                    return true
                }

                if (selectedUnit!!.isPlayerTeamUnit(player)
                    && selectedUnit!!.isPlayerUnit(player)
                    && !unit.isPlayerTeamUnit(player)
                    && selectedUnit!!.inAttackRange(unit.tiledX, unit.tiledY)
                    && !selectedUnit!!.isAlly(unit)
                ) {

                    val command = AttackCommand(selectedUnit!!, unit)
                    if (command.canExecute(playScreen) && playScreen.commandManager.queueCommand(command)) {
                        selectUnit(selectedUnit)
                        return true
                    }
                } else selectUnit(unit)
            }

            return false
        }
    }

    inner class UseAbilityCs : ClickStrategy {
        override fun onMapClicked(tiledX: Int, tiledY: Int): Boolean {
            if (playStage.isOutOfMap(tiledX, tiledY)) return false

            if (abilitiesPanel.selectedAbility!!.skipConfirmation) {
                val command = UseAbilityCommand(selectedUnit!!, abilitiesPanel.selectedAbility!!, tiledX, tiledY)

                val isExecuted = command.canExecute(playScreen) && playScreen.commandManager.queueCommand(command)

                if (isExecuted && abilitiesPanel.selectedAbility is SummonAbility && abilitiesPanel.selectedAbility!!.castAmountsLeft > 1) {
                    clickStrategy = useAbilityCs
                } else
                    selectUnit(selectedUnit)

                return isExecuted
            } else {
                if (tiledDst(
                        tiledX,
                        tiledY,
                        selectedUnit!!.tiledX,
                        selectedUnit!!.tiledY
                    ) <= abilitiesPanel.selectedAbility!!.range
                ) {
                    clickStrategy = confirmAbilityCs
                    abilityActivationRangeBorder.makeForRange(
                        abilitiesPanel.selectedAbility!!.activationRange,
                        tiledX,
                        tiledY,
                        playStage
                    )
                    abilityActivationRangeBorder.show(true)
                    return true
                } else {

                    clickStrategy = unitSelectedCs
                    selectUnit(selectedUnit)
                }
            }

            return false
        }

        override fun onTileClicked(tile: TileActor): Boolean {

            return false
        }

        override fun onUnitClicked(unit: UnitActor): Boolean {
            if (abilitiesPanel.selectedAbility!!.skipConfirmation)
                selectUnit(null)

            return false
        }
    }

    inner class ConfirmAbilityCs : ClickStrategy {

        override fun onTileClicked(tile: TileActor): Boolean {
            selectUnit(selectedUnit)
            return false
        }

        override fun onUnitClicked(unit: UnitActor): Boolean {
            selectUnit(selectedUnit)
            return false
        }

        override fun onMapClicked(tiledX: Int, tiledY: Int): Boolean {
            //selectUnit(selectedUnit)

//            if (tileSelectionDrawer.hoveringActor?.tiledX != tiledX || tileSelectionDrawer.hoveringActor?.tiledY != tiledY)
//                return false

            val command = UseAbilityCommand(selectedUnit!!, abilitiesPanel.selectedAbility!!, tiledX, tiledY)

            val isExecuted = command.canExecute(playScreen) && playScreen.commandManager.queueCommand(command)

            if (isExecuted && abilitiesPanel.selectedAbility is SummonAbility && abilitiesPanel.selectedAbility!!.castAmountsLeft > 0) {
                clickStrategy = confirmAbilityCs
            }

            return isExecuted
        }
    }
}
