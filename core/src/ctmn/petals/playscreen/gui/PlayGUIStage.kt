package ctmn.petals.playscreen.gui

import com.badlogic.gdx.Gdx
import com.badlogic.gdx.Input
import com.badlogic.gdx.graphics.Color
import com.badlogic.gdx.graphics.OrthographicCamera
import com.badlogic.gdx.scenes.scene2d.Action
import com.badlogic.gdx.scenes.scene2d.Actor
import com.badlogic.gdx.scenes.scene2d.InputEvent
import com.badlogic.gdx.scenes.scene2d.Stage
import com.badlogic.gdx.scenes.scene2d.utils.ChangeListener
import com.badlogic.gdx.scenes.scene2d.utils.ClickListener
import com.badlogic.gdx.utils.Align
import com.badlogic.gdx.utils.viewport.ExtendViewport
import com.kotcrab.vis.ui.VisUI
import com.kotcrab.vis.ui.widget.VisImage
import com.kotcrab.vis.ui.widget.VisImageButton
import com.kotcrab.vis.ui.widget.VisLabel
import com.kotcrab.vis.ui.widget.VisTable
import ctmn.petals.AudioManager
import ctmn.petals.Const
import ctmn.petals.Const.BASE_BUILD_COST
import ctmn.petals.Const.PLAY_GUI_VIEWPORT_HEIGHT
import ctmn.petals.Const.PLAY_GUI_VIEWPORT_WIDTH
import ctmn.petals.Const.TILE_SIZE
import ctmn.petals.GamePref
import ctmn.petals.editor.ui.addTooltip
import ctmn.petals.multiplayer.ClientPlayScreen
import ctmn.petals.player.Player
import ctmn.petals.player.Species
import ctmn.petals.player.fairy
import ctmn.petals.playscreen.*
import ctmn.petals.playscreen.commands.*
import ctmn.petals.playscreen.events.*
import ctmn.petals.playscreen.gui.widgets.*
import ctmn.petals.playscreen.seqactions.CameraMoveAction
import ctmn.petals.playscreen.seqactions.UpdateAction
import ctmn.petals.playscreen.tasks.DialogTask
import ctmn.petals.playstage.*
import ctmn.petals.story.aliceOrNull
import ctmn.petals.tile.TileActor
import ctmn.petals.tile.cPlayerId
import ctmn.petals.tile.components.ActionCooldown
import ctmn.petals.tile.isBase
import ctmn.petals.tile.isCapturable
import ctmn.petals.unit.*
import ctmn.petals.unit.abilities.SummonAbility
import ctmn.petals.unit.component.WaypointComponent
import ctmn.petals.utils.*
import ctmn.petals.widgets.addChangeListener
import ctmn.petals.widgets.newIconButton
import ctmn.petals.widgets.newLabel


class PlayGUIStage(
    val playScreen: PlayScreen,
) : Stage(ExtendViewport(PLAY_GUI_VIEWPORT_WIDTH, PLAY_GUI_VIEWPORT_HEIGHT), playScreen.batch) {
    /** see [onScreenResize] */

    val assets = playScreen.assets

    //localPlayer
    val localPlayer: Player get() = playScreen.localPlayer

    //stage
    val playStage = playScreen.playStage
    val playStageCamera = playScreen.playStage.camera as OrthographicCamera

    /// Widgets
    //tables
    private val dialogTable = VisTable()
    val turnIconTable = VisTable()
    val abilityChooseDialogTable = VisTable()
    val buyMenu = BuyMenu()

    //panels and windows and stuff
    val unitPanel = UnitPanel(this)

    val tasksTable = TasksTable()
    private val turnIcon = TurnIcon()

    //labels
    var showCredits: Boolean
        get() = creditsLabel.isVisible && creditsIcon.isVisible
        set(value) {
            creditsIcon.isVisible = value
            creditsLabel.isVisible = value
        }
    private val creditsIcon = VisImage("credits")
    private val creditsLabel =
        newLabel("${localPlayer.credits}", "default").apply {
            isVisible = playScreen.gameMode == GameMode.CRYSTALS || playScreen.gameMode == GameMode.CRYSTALS_LEADERS
            creditsIcon.isVisible = isVisible
        }
    private val creditsLabelTooltip = addTooltip(creditsLabel, "()", Align.bottom)
    private val fpsLabel = VisLabel().apply {
        isVisible = Const.SHOW_FPS
    }

    //buttons
    val charactersPanel = CharactersPanel(this)
    val bookButton = newIconButton("book").apply {
        addActor(VisLabel("").apply { name = "notify"; setPosition(16f, 4f) })
    }
    val potionButton = newIconButton("potion")
    val hideUiButton = newIconButton("hide_ui")
    private val zoomButton = newIconButton("zoom").apply { isVisible = false }
    private val infoButton = newIconButton("info").apply { isVisible = false }
    private val pauseButton = newIconButton("pause")
    val endTurnButton = newIconButton("end_turn")
    val unitsHaveActionsPanel = AwaitingOrderPanel(this)
    private val unitsHaveWaypointButton = newIconButton("units_have_waypoint")
    private val backButtonStyle = VisUI.getSkin().get("back", VisImageButton.VisImageButtonStyle::class.java)
    private val cancelButtonStyle =
        VisUI.getSkin().get("cancel_in-game", VisImageButton.VisImageButtonStyle::class.java)
    private val cancelButton: VisImageButton = newIconButton("back").apply { isVisible = false }
    private val refreshButton = newIconButton("refresh")
    val abilitiesPanel = AbilitiesPanel(this)
    private val unitMiniMenu = UnitMiniMenu(this)
    val nextDialogButton = StoryDialog.NextDialogButton(this)
    private val captureButton = newIconButton("capture").apply { isVisible = false }
    val buildBaseButton = newIconButton("build_base").apply {
        isVisible = false
        addActor(VisLabel(BASE_BUILD_COST.toString()).apply { setPosition(16f, 4f) })
    }
    private val buildButton = newIconButton("build").apply { isVisible = false }
    private val destroyTileButton = newIconButton("destroy_tile").apply { isVisible = false }
    private val waypointButton = newIconButton("waypoint").apply { isVisible = false }

    //not widgets
    val actorHighlighter = ActorHighlighter(this)
    //playstage drawers
    val tileSelectionDrawer = TileSelectionDrawer(this)
    private val attackIconsDrawer = AttackIconsDrawer(this).also { it.isVisible = true }
    private val iconsDrawer = IconsDrawer(this)
    private val leaderFieldDrawer = LeaderFieldDrawer(this)
    private val unitInfoDrawer = UnitInfoDrawer(this)
    private val animationsUpdater = AnimationsUpdater(this)
    val movementCostDrawer = MovementCostDrawer(this)
    private val terrainBonusDrawer = TerrainBonusDrawer(this)
    val tileHighlighter = TileHighlighter(this)

    //ability and movement borders (playStage)
    val movementBorder = AttackMovementRangeDrawer(this)
    val abilityRangeBorder = BorderDrawer(Color.VIOLET, this)
    val abilityActivationRangeBorder = BorderDrawer(Color.VIOLET, this)

    //state
    val myTurn = State("myTurn")
    val endTurn = State("endTurn")
    val startTurn = State("startTurn")
    val theirTurn = State("theirTurn")
    val nextPlayerPrepare = State("nextPlayerPrepare")
    val gameOverState = State("gameOverState")
    private val loseState = State("loseState")
    private val winState = State("winState")
    var currentState: State = State("initialState")
        set(value) {
            field.onExit?.invoke()
            value.onEnter?.invoke()

            logMsg("State changed: ${field.name} -> ${value.name}")

            field = value
        }

    private val holdStateTimer = Timer(0f)

    /** [MapClickListener]s */
    val blockedCL = BlockedCL()
    val seeInfoCL = SeeInfoCL()
    val selectUnitCL = SelectUnitCL()
    val unitSelectedCL = UnitSelectedCL()
    val useAbilityCL = UseAbilityCL()
    val confirmAbilityCL = ConfirmAbilityCL()

    /** Previously selected unit between UnitSelectedCL and other CL*/
    private var prevSelectedUnit: UnitActor? = null

    var mapClickListener: MapClickListener = selectUnitCL
        set(value) {

            //hide borders on exit
            when {
                field is ConfirmAbilityCL && value !is ConfirmAbilityCL -> {
                    abilityActivationRangeBorder.isVisible = false
                }

                field is UseAbilityCL && value !is UseAbilityCL -> {
                    abilityRangeBorder.isVisible = false

                    tileHighlighter.clearHighlights()
                    (abilitiesPanel.cells.firstOrNull { it.actor is SummonAbilityButton }?.actor as SummonAbilityButton?)?.hidePane()
                }

                field is UnitSelectedCL && (value is SelectUnitCL || value is SeeInfoCL) -> {
                    // fire event
                    AudioManager.sound("unit_deselect")
                    prevSelectedUnit = null
                }
            }

            when (value) {
                is UnitSelectedCL -> {
                    if (selectedUnit != null) {
                        if (prevSelectedUnit != selectedUnit) {
                            AudioManager.sound("unit_select")
                        } else {
                            // assuming unit just moved
//                            if (selectedUnit!!.actionPoints >= Const.ACTION_POINTS_ATTACK)
//                            AudioManager.sound("unit_deselect")
                        }
                    }

                    prevSelectedUnit = selectedUnit
                }

                is UseAbilityCL -> {
                    if (selectedUnit == null) throw IllegalStateException("While mapClickListener is UseAbilityCL, selectedUnit shouldn't be null.")

                    abilityRangeBorder.isVisible = true

                    tileHighlighter.clearHighlights()

                    if (abilitiesPanel.selectedAbility is SummonAbility) {
                        tileHighlighter.highlightSummoning(selectedUnit!!, 1)

                        abilityRangeBorder.isVisible = false
                    }
                }

                is ConfirmAbilityCL -> {
                    abilityActivationRangeBorder.isVisible = true
                }
            }

            logMsg("MapClickListener changed: ${field::class.simpleName} -> ${value::class.simpleName}")

            if (field !is BlockedCL) //why
                field = value
        }

    //use selectUnit(unit)
    var selectedUnit: UnitActor? = null
    val isUnitSelected get() = selectedUnit != null

    var mapClickDisabled = false

    private val autoEndTurnEnabled get() = playScreen.gameType != GameType.MULTIPLAYER && GamePref.autoEndTurn

    var forceMyTurnEnd = false

    val inputManager = InputManager(this)

    init {
        playStage.zoomCameraByDefault()
        playStage.centerCameraByDefault()

        //setup actors
        setupPlayStage()

        addActor(animationsUpdater)

        addActor(unitPanel.table)
        addActor(turnIconTable)

        setupTable()
        addActor(dialogTable)

        abilityChooseDialogTable.setFillParent(true)
        addActor(abilityChooseDialogTable)

        addActor(buyMenu)
        addActor(unitMiniMenu)

        addActor(charactersPanel)

        addActor(actorHighlighter)

        //add widgets listeners

        hideUiButton.addChangeListener {
            hideUi()
        }

        bookButton.addChangeListener {
            addCover()
            addActor(Book(this@PlayGUIStage))
            bookButton.findActor<VisLabel>("notify").setText("")
        }

        potionButton.addChangeListener {
            addCover()
            addActor(Potion(this@PlayGUIStage))
        }

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

                addActor(InGameMenu(playScreen))
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
                endTurn()
            }
        })

        captureButton.addChangeListener {
            selectedUnit?.let { unit ->
                if (unit.isPlayerUnit(localPlayer)) {
                    val tile = playStage.getTile(unit.tiledX, unit.tiledY) ?: return@addChangeListener
                    if (!tile.isCapturable) return@addChangeListener
                    val captureCommand = CaptureCommand(unit.name, tile.name)
                    if (!captureCommand.canExecute(playScreen)) return@addChangeListener

                    playScreen.queueCommand(captureCommand)
                }
            }

            captureButton.isVisible = false
        }

        destroyTileButton.addChangeListener {
            selectedUnit?.let { unit ->
                if (unit.isPlayerUnit(localPlayer)) {
                    val tile = playStage.getTile(unit.tiledX, unit.tiledY) ?: return@addChangeListener
                    if (!unit.canDestroy(tile) || tile.cPlayerId?.playerId == unit.playerId || playScreen.turnManager.getPlayerById(
                            tile.cPlayerId?.playerId ?: -1
                        )?.isAlly(unit.playerId) == true
                    ) return@addChangeListener
                    val buildCommand = DestroyTileCommand(unit.name, tile.name)
                    if (!buildCommand.canExecute(playScreen)) return@addChangeListener

                    playScreen.queueCommand(buildCommand)
                }
            }

            destroyTileButton.isVisible = false
        }

        waypointButton.addChangeListener {
            if (playScreen.turnManager.currentPlayer != localPlayer) return@addChangeListener

            selectedUnit?.del(WaypointComponent::class.java)

            isSettingWaypoint = !isSettingWaypoint
        }

        buildBaseButton.addChangeListener {
            selectedUnit?.let { unit ->
                if (unit.isPlayerUnit(localPlayer)) {
                    val tile = playStage.getTile(unit.tiledX, unit.tiledY) ?: return@addChangeListener
                    if (!unit.canBuildBase(tile)) return@addChangeListener
                    val buildCommand = BuildBaseCommand(unit.name, tile.name)
                    if (!buildCommand.canExecute(playScreen)) return@addChangeListener

                    playScreen.queueCommand(buildCommand)
                }
            }

            buildBaseButton.isVisible = false
        }

        buildButton.addChangeListener {
            selectedUnit?.let { unit ->
                if (unit.isPlayerUnit(localPlayer)) {
                    addActor(
                        BuildMenu(
                            this@PlayGUIStage,
                            unit,
                            playStage.getTile(unit)!!,
                            localPlayer,
                            Species.getSpeciesBuildings(localPlayer.species)
                        )
                    )
                }
            }

            buildButton.isVisible = false
        }

        addListener {
            val unit: UnitActor? = when (it) {
                is UnitSelectedEvent -> it.unit
                is UnitMovedEvent -> it.unit
                else -> return@addListener false
            }

            captureButton.isVisible = false
            buildBaseButton.isVisible = false
            buildButton.isVisible = false
            destroyTileButton.isVisible = false
            waypointButton.isVisible = false

            unit ?: return@addListener false

            if (unit.isPlayerUnit(localPlayer) && unit.actionPoints > 0) {
                waypointButton.isVisible = true

                val tile = playStage.getTile(unit.tiledX, unit.tiledY) ?: return@addListener false

                buildBaseButton.isVisible = unit.canBuildBase()
                if (buildBaseButton.isVisible) {
                    buildBaseButton.style.imageUp = if (localPlayer.species == fairy) VisUI.getSkin().getDrawable("icons/pixie_nest")
                    else VisUI.getSkin().getDrawable("icons/goblin_den")
                }
                buildBaseButton.isDisabled = !unit.canBuildBase(tile) || localPlayer.credits < BASE_BUILD_COST

                buildButton.isVisible = unit.canBuild()
                buildButton.isDisabled = !unit.canBuild(tile)

                destroyTileButton.isVisible = unit.canDestroy(
                    tile,
                    playScreen.turnManager.getPlayerById(tile.cPlayerId?.playerId ?: -1)?.teamId
                )
                destroyTileButton.isDisabled = !unit.canDestroy(
                    tile,
                    playScreen.turnManager.getPlayerById(tile.cPlayerId?.playerId ?: -1)?.teamId
                )

                if (!tile.isCapturable) return@addListener false

                captureButton.isVisible = unit.canCapture()

                if (tile.cPlayerId?.playerId == localPlayer.id) {
                    //captureButton.isDisabled = true
                    captureButton.isVisible = false
                    return@addListener false
                }

                captureButton.isDisabled = !unit.canCapture(tile)

                //if (unit.canCapture(tile)) return@addListener false
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
            mapClickListener = seeInfoCL

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
            mapClickListener = selectUnitCL

            endTurnButton.isDisabled = false

            if (forceMyTurnEnd)
                playScreen.commandManager.queueCommand(EndTurnCommand(localPlayer))
        }

        myTurn.onExit = {
            endTurnButton.isDisabled = true
        }

        // End turn
        endTurn.update = {
            if (holdStateTimer.isDone) {
                currentState = if (playScreen.gameType == GameType.PVP_SAME_SCREEN) {
                    if (!playScreen.botManager.isBotPlayer(playScreen.turnManager.currentPlayer)) {
                        nextPlayerPrepare
                    } else {
                        theirTurn
                    }
                } else {
                    theirTurn
                }
            }
        }

        endTurn.onExit = {

        }

        endTurn.onEnter = {
            if (playScreen.gameType == GameType.PVP_SAME_SCREEN)
                if (!playScreen.botManager.isBotPlayer(playScreen.turnManager.nextPlayer))
                    if (playScreen.turnManager.players.filter { !playScreen.botManager.isBotPlayer(it) && !it.isOutOfGame }.size > 1)
                        playScreen.fogOfWarManager.hideAll = true

            mapClickListener = seeInfoCL

            holdStateTimer.start(1f)

            selectUnit(null)
        }

        // Their turn
        theirTurn.onEnter = {
            mapClickListener = seeInfoCL
        }

        // Game Over
        gameOverState.onEnter = {
            mapClickListener = blockedCL
        }

        // Lose
        loseState.onEnter = {
            mapClickListener = blockedCL
        }

        // Win
        winState.onEnter = {
            mapClickListener = blockedCL
        }

        // Next Player
        nextPlayerPrepare.onEnter = {
            // if game type is not same screen or there are less than 2 P&P players, do as usual
            if (playScreen.gameType != GameType.PVP_SAME_SCREEN || (playScreen.turnManager.players.filter {
                    !playScreen.botManager.isBotPlayer(
                        it
                    ) && !it.isOutOfGame
                }.size < 2)) {
                playScreen.fogOfWarManager.hideAll = false
                endTurnButton.isDisabled = false
            } else {
                //if same screen and >1 p&p players, hide units and add player ready label

                mapClickListener = seeInfoCL

                playScreen.fogOfWarManager.hideAll = true

                val labelPlayerReady = newLabel(
                    "Player ${playerColorName(playScreen.turnManager.currentPlayer.id).replaceFirstChar { it.uppercaseChar() }} Ready",
                    "default"
                )
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
        }

        nextPlayerPrepare.update = {
            if (currentState == nextPlayerPrepare)
                endTurnButton.isDisabled = true

            if (!playScreen.fogOfWarManager.hideAll)
                currentState = startTurn
        }

        nextPlayerPrepare.onExit = {
            playScreen.fogOfWarManager.hideAll = false

            endTurnButton.isDisabled = false
        }

        currentState = if (playScreen.turnManager.currentPlayer == localPlayer) myTurn else theirTurn
        // States setup ^

        // disable or enable end turn button
        addListener {
            when (it) {
                is ActionStartedEvent -> {
                    endTurnButton.isDisabled = true
                }

                is ActionCompletedEvent -> {
                    if (playScreen.turnManager.currentPlayer == localPlayer
                        && !playScreen.actionManager.hasActions
                    )
                        endTurnButton.isDisabled = false
                }

                is TaskBeginEvent -> {
                    if (it.task.isForcePlayerToComplete || it.task is DialogTask) {
                        endTurnButton.isDisabled = true

                        it.task.addOnCompleteTrigger {
                            for (task in playScreen.taskManager.getTasks())
                                if (task.isForcePlayerToComplete || task is DialogTask)
                                    return@addOnCompleteTrigger

                            if (!playScreen.actionManager.hasActions && playScreen.turnManager.currentPlayer == localPlayer)
                                endTurnButton.isDisabled = false
                        }
                    }
                }

                is NextTurnEvent -> {
                    endTurnButton.isDisabled =
                        playScreen.actionManager.hasActions || playScreen.turnManager.currentPlayer != localPlayer
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
                            /**see [nextPlayerPrepare] */
                            if (playScreen.botManager.isBotPlayer(playScreen.turnManager.previousPlayer)) {
                                if (playScreen.turnManager.currentPlayer.id == localPlayer.id)
                                    currentState = nextPlayerPrepare
                            }
                        }

                        else -> {
                            if (playScreen.turnManager.currentPlayer.id == localPlayer.id)
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
                    if (playScreen.turnManager.currentPlayer == localPlayer
                        && tile.cPlayerId?.playerId == localPlayer.id
                        && currentState == myTurn
                    )
                        buyMenu.show(it.tile, localPlayer)
                }
            }

            false
        }

        //a listener that performs unit commands on an event depending on mapClickListener value
        addListener { event ->
            when (event) {
                is MapClickedEvent -> {
                    mapClickListener.onMapClicked(event.clickX.tiled(), event.clickY.tiled())
                }

                is TileClickedEvent -> {
                    mapClickListener.onTileClicked(event.tile)
                }

                is UnitClickedEvent -> {
                    mapClickListener.onUnitClicked(event.unit)
                }
            }

            false
        }

        //autoEndTurn
        addListener { event ->
            if (!autoEndTurnEnabled) return@addListener false
            if (playScreen.turnManager.currentPlayer != localPlayer) return@addListener false
            if (playScreen.actionManager.hasActions) return@addListener false
            if (event is CommandExecutedEvent || event is ActionCompletedEvent) {
                if (event is CommandExecutedEvent) {
                    if (event.command is EndTurnCommand) return@addListener false
                }
                var actionAvailable = playStage.playerUnitsHasAction(localPlayer).size > 0
                if (
                    playStage.getCapturablesOf(localPlayer)
                        .any { it.isBase && !it.has(ActionCooldown::class.java) } && localPlayer.credits >= 50
                )
                    actionAvailable = true

                if (!actionAvailable) endTurn()
            }

            false
        }

        addCaptureListener {
            if (it is InputEvent && nextDialogButton.hasDialogs() && it.target != pauseButton  && it.target != nextDialogButton && it.target !is StoryDialog && it.target.name != StoryDialog.PC_ENTER_BUTTON_NAME) {
                if (root.findActor<InGameMenu>("game_menu") != null) return@addCaptureListener true
                if (it.type == InputEvent.Type.keyDown && it.keyCode == Input.Keys.ENTER) {
                    true
                } else {
                    it.cancel()
                    false
                }
            } else {
                false
            }
        }
    }

    private val unitsToMove = mutableListOf<UnitActor>()

    private fun endTurn() {
        for (task in playScreen.taskManager.getTasks()) {
            if (task.isForcePlayerToComplete)
                return
        }

        if (playScreen.actionManager.hasActions) return

        unitsToMove.clear()
        unitsToMove.addAll(
            playStage.getUnitsOfPlayer(localPlayer).filter { it.canMove() && it.has(WaypointComponent::class.java) })
        if (unitsToMove.isEmpty()) {
            playScreen.commandManager.queueCommand(EndTurnCommand(localPlayer))
        } else {
            playScreen.addAction(UpdateAction {
                val unitsToRemove = mutableListOf<UnitActor>()
                for (unit in unitsToMove) {
                    val waypoint = unit.get(WaypointComponent::class.java)!!
                    val closestTile =
                        unit.getClosestTileInMoveRange(waypoint.tileX, waypoint.tileY, includeUnitPosTile = true)
                    if (closestTile != null) {
                        val command = MoveUnitCommand(unit, closestTile.tiledX, closestTile.tiledY)
                        if (command.canExecute(playScreen)) {
                            if (command.tileX == waypoint.tileX && command.tileY == waypoint.tileY) {
                                unit.del(WaypointComponent::class.java)
                            }
                            playScreen.queueCommand(command)
                        } else {
                            unit.actionPoints = 0
                        }
                    } else {
                        unit.actionPoints = 0
                    }

                    unitsToRemove.add(unit)
                    break
                }

                unitsToMove.removeAll(unitsToRemove)

                if (unitsToMove.isEmpty()
                    || playScreen.turnManager.currentPlayer != localPlayer
                    || unitsToMove.none { it.has(WaypointComponent::class.java) || it.canMove() }
                ) {
                    playScreen.commandManager.queueCommand(EndTurnCommand(localPlayer))
                    true
                } else {
                    false
                }
            })
        }
    }

    private fun setupPlayStage() {
        if (playStage.root.findActor<Actor>("been_set_up_mark") != null) {
            logErr("PlayStage UI is already set up.")
            return
        }

        logMsg("PlayStage UI set up.")

        playStage.addActor(Actor().apply { name = "been_set_up_mark" })

        //actors
        playStage.addActor(attackIconsDrawer)
        playStage.addActor(tileHighlighter)
        playStage.addActor(tileSelectionDrawer)
        playStage.addActor(iconsDrawer)
        playStage.addActor(unitInfoDrawer)
        playStage.addActor(movementCostDrawer)
        playStage.addActor(terrainBonusDrawer)
        playStage.addActorAfterTiles(movementBorder)
        playStage.addActorAfterTiles(abilityRangeBorder)
        playStage.addActorAfterTiles(abilityActivationRangeBorder)
        playStage.addActorAfterTiles(leaderFieldDrawer)

        //listeners
        playStage.addListener {
            if (it is CommandExecutedEvent || it is NextTurnEvent || it is CreditsChangeEvent) {
                creditsLabel.setText(
                    "${localPlayer.credits} (+${localPlayer.income(playScreen)}/${
                        localPlayer.incomeReserve(
                            playScreen
                        )
                    })"
                )

                creditsLabelTooltip.setText("")//${localPlayer.incomeReserve(playScreen)}
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
            if (it is UnitLevelUpEvent && it.unit.playerId == localPlayer.id) {
                abilityChooseDialogTable.add(LevelUpWindow(it.unit)).center() //AbilitiesSelectTable(it.unit)
            }

            return@addListener false
        }

        //select Bot unit in action
        playStage.addListener { event ->
            if (!GamePref.showBotGui) return@addListener false

            if (playScreen.botManager.isBotPlayer(playScreen.turnManager.currentPlayer)) {
                when (event) {
                    is CommandAddedEvent -> {
                        val unit = when (val command = event.command) {
                            is MoveUnitCommand ->
                                if (playScreen.fogOfWarManager.isVisible(command.tileX, command.tileY))
                                    playStage.findUnit(command.unitId) else null

                            is AttackCommand -> playStage.findUnit(command.attackerUnitId)
                            is CaptureCommand -> playStage.findUnit(command.unitId)
                            else -> null
                        }
                        if (unit != null && unit.isPlayerUnit(playScreen.turnManager.currentPlayer)
                            && playScreen.fogOfWarManager.isVisible(unit.tiledX, unit.tiledY)
                        ) {
                            selectUnit(unit)
                        }
                    }

                    is UnitBoughtEvent -> {
                        val unit = event.unit
                        if (unit.isPlayerUnit(playScreen.turnManager.currentPlayer)
                            && playScreen.fogOfWarManager.isVisible(unit.tiledX, unit.tiledY)
                        ) {
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
            top().right().padRight(3f).padTop(3f)
            add(fpsLabel).top().right().padRight(20f)
            add(creditsIcon).right().top().padTop(3f)
            add(creditsLabel).top().right().padRight(3f).height(creditsIcon.height).padTop(3f)
            add(topRightButtonsTable).top().right()
        }

        with(createTable()) {
            bottom()
            add(VisTable().apply {
                add(captureButton)
                add(buildBaseButton)
                add(buildButton)
                add(destroyTileButton)
                add(waypointButton)
            })
            row()
            add(abilitiesPanel)
        }
        createTable(endTurnButton).bottom().right()

//        with(createTable()) {
//            center().right().padRight(2f)
//            add(unitsHaveActionButton)
//        }

        with(turnIconTable) {
            top()
            add(turnIcon).top().center()
            row()
            //add(turnIcon.label).top().center()
        }

        addActor(VisTable().apply {
            setFillParent(true)
            top()
            left()
            add(VisTable().apply {
                add(hideUiButton).size(60f)
                add(bookButton).size(60f)
                add(potionButton).size(60f)
            }).left()
            row()
            add(tasksTable).left().top()
        })

        addActor(NextTurnPopUp(this))
    }

    override fun keyDown(keyCode: Int): Boolean {
        when (keyCode) {
            Input.Keys.ESCAPE -> {
                if (root.findActor<InGameMenu>("game_menu")?.remove() == null)
                    addActor(InGameMenu(playScreen))

                return true
            }

            Input.Keys.ENTER -> {
                if (!endTurnButton.isDisabled && endTurnButton.isVisible)
                    endTurn()
            }

            Input.Keys.SPACE -> {
                if (playScreen.turnManager.currentPlayer == localPlayer && !playScreen.actionManager.hasActions)
                    selectNextAvailableUnit()
            }

            Input.Keys.M -> {
                selectedUnit?.let { selectedUnit ->
                    if (selectedUnit.playerId == localPlayer.id) {
                        if (selectedUnit.del(WaypointComponent::class.java) == null) {
                            selectedUnit.add(
                                WaypointComponent(
                                    tileSelectionDrawer.hoveringSprite.centerX().tiled(),
                                    tileSelectionDrawer.hoveringSprite.centerY().tiled()
                                )
                            )
                        }
                    }
                }
            }
        }

        return super.keyDown(keyCode)
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
        currentState = when (localPlayer) {
            playScreen.turnManager.currentPlayer -> myTurn
            else -> theirTurn
        }
        selectUnit(null)
    }

    fun selectUnit(unit: UnitActor?) {
        isSettingWaypoint = false

        selectedUnit =
            if (unit != null && (unit.stage == null || !unit.isAlive() || !playScreen.fogOfWarManager.isVisible(
                    unit.tiledX,
                    unit.tiledY
                ))
            )
                null
            else
                unit

        root.fire(UnitSelectedEvent(selectedUnit))

        if (selectedUnit == null)
            cancelButton.style = backButtonStyle
        else
            cancelButton.style = cancelButtonStyle

        if (selectedUnit == null) {
            mapClickListener = if (myTurn.isCurrent()) selectUnitCL else seeInfoCL

            abilitiesPanel.setUpForUnit(null)
            //unitMiniMenu.unit = null
            return
        }

        if (!selectedUnit!!.isPlayerUnit(localPlayer) || !myTurn.isCurrent()) {
            abilitiesPanel.setUpForUnit(null)

            mapClickListener = seeInfoCL
            return
        }

        mapClickListener = unitSelectedCL

        if (selectedUnit!!.isPlayerUnit(localPlayer)) {
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

    fun showUi() {
        root.isVisible = true
        unitInfoDrawer.isVisible = true
    }

    fun hideUi() {
        root.isVisible = false
        unitInfoDrawer.isVisible = false
    }

    /** Fires @MapClickedEvent and, if mapClickListener has not changed, UnitClickedEvent TileClickedEvent */
    fun onMapClicked(clickX: Float, clickY: Float) {
        showUi()

        if (mapClickDisabled) return

        val tiledX: Int = (clickX / TILE_SIZE).toInt()
        val tiledY: Int = (clickY / TILE_SIZE).toInt()

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

        val oldClickListener = mapClickListener

        root.fire(MapClickedEvent(clickX, clickY))

        if (oldClickListener == mapClickListener) {
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

    fun selectNextAvailableUnit() {
        val plUnits = playStage.playerUnitsHasAction(localPlayer)
        if (plUnits.isEmpty) return
        if (selectedUnit != null) {
            val unitIndex = plUnits.indexOf(selectedUnit)
            selectUnit(null)
            if (unitIndex > -1) {
                val nextIndex = if (unitIndex + 1 < plUnits.size) unitIndex + 1 else 0
                selectUnit(plUnits.get(nextIndex))
            } else {
                selectUnit(plUnits.first())
            }
        } else {
            selectUnit(plUnits.first())
        }
    }

    fun setWaypointAtCursorPos() {
        selectedUnit?.let { selectedUnit ->
            if (selectedUnit.playerId == localPlayer.id) {
                if (selectedUnit.del(WaypointComponent::class.java) == null) {
                    selectedUnit.add(
                        WaypointComponent(
                            tileSelectionDrawer.hoveringSprite.centerX().tiled(),
                            tileSelectionDrawer.hoveringSprite.centerY().tiled()
                        )
                    )
                }
            }
        }
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

        unitPanel.onScreenResize(width, height)
    }

    /** What da click doin */
    interface MapClickListener {
        fun onMapClicked(tiledX: Int, tiledY: Int): Boolean = false
        fun onTileClicked(tile: TileActor): Boolean = false
        fun onUnitClicked(unit: UnitActor): Boolean = false
    }

    inner class BlockedCL : MapClickListener

    inner class SeeInfoCL : MapClickListener {
        override fun onTileClicked(tile: TileActor): Boolean {
            selectUnit(null)

            return false
        }

        override fun onUnitClicked(unit: UnitActor): Boolean {
            selectUnit(unit)

            return false
        }
    }

    inner class SelectUnitCL : MapClickListener {
        override fun onUnitClicked(unit: UnitActor): Boolean {
            if (myTurn.isCurrent()) {
                selectUnit(unit)
                return true
            }

            return false
        }
    }

    var isSettingWaypoint = false

    inner class UnitSelectedCL : MapClickListener {
        override fun onTileClicked(tile: TileActor): Boolean {
            if (myTurn.isCurrent()) {
                if (isSettingWaypoint) {
                    setWaypointAtCursorPos()
                    isSettingWaypoint = false
                    return true
                }

                if (selectedUnit!!.canMove()) {
                    val command = MoveUnitCommand(selectedUnit!!, tile.tiledX, tile.tiledY)
                    if (command.canExecute(playScreen) && playScreen.commandManager.queueCommand(command)) {
                        selectUnit(selectedUnit)
                        return true
                    } else selectUnit(null)
                } else selectUnit(null)
            }

            return false
        }

        override fun onUnitClicked(unit: UnitActor): Boolean {
            if (myTurn.isCurrent()) {
                if (selectedUnit == unit) {
                    selectUnit(null)
                    return true
                }

                if (selectedUnit!!.isPlayerTeamUnit(localPlayer)
                    && selectedUnit!!.isPlayerUnit(localPlayer)
                    && !unit.isPlayerTeamUnit(localPlayer)
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

    inner class UseAbilityCL : MapClickListener {
        override fun onMapClicked(tiledX: Int, tiledY: Int): Boolean {
            if (playStage.isOutOfMap(tiledX, tiledY)) return false

            if (abilitiesPanel.selectedAbility!!.skipConfirmation) {
                val command = UseAbilityCommand(selectedUnit!!, abilitiesPanel.selectedAbility!!, tiledX, tiledY)

                val isExecuted = command.canExecute(playScreen) && playScreen.commandManager.queueCommand(command)

                if (isExecuted && abilitiesPanel.selectedAbility is SummonAbility && abilitiesPanel.selectedAbility!!.castAmountsLeft > 1) {
                    mapClickListener = useAbilityCL
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
                    mapClickListener = confirmAbilityCL
                    abilityActivationRangeBorder.makeForRange(
                        abilitiesPanel.selectedAbility!!.activationRange,
                        tiledX,
                        tiledY,
                        playStage
                    )
                    abilityActivationRangeBorder.show(true)
                    return true
                } else {

                    mapClickListener = unitSelectedCL
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

    inner class ConfirmAbilityCL : MapClickListener {

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
                mapClickListener = confirmAbilityCL
            }

            return isExecuted
        }
    }
}
