package ctmn.petals.menu

import com.badlogic.gdx.Gdx
import com.badlogic.gdx.graphics.g2d.Batch
import com.badlogic.gdx.scenes.scene2d.Actor
import com.badlogic.gdx.scenes.scene2d.InputEvent
import com.badlogic.gdx.scenes.scene2d.InputListener
import com.badlogic.gdx.scenes.scene2d.Stage
import com.badlogic.gdx.scenes.scene2d.ui.WidgetGroup
import com.badlogic.gdx.utils.Align
import com.badlogic.gdx.utils.Array
import com.kotcrab.vis.ui.VisUI
import com.kotcrab.vis.ui.widget.VisCheckBox
import com.kotcrab.vis.ui.widget.VisScrollPane
import com.kotcrab.vis.ui.widget.VisTable
import com.kotcrab.vis.ui.widget.VisWindow
import ctmn.petals.assets
import ctmn.petals.editor.isOutdatedVersion
import ctmn.petals.map.MapConverted
import ctmn.petals.map.labels
import ctmn.petals.map.label.LabelActor
import ctmn.petals.multiplayer.json.PlayerSlotState
import ctmn.petals.playstage.PlayStage
import ctmn.petals.playscreen.GameMode
import ctmn.petals.playscreen.Season
import ctmn.petals.player.Player
import ctmn.petals.player.Species
import ctmn.petals.player.fairy
import ctmn.petals.player.goblin
import ctmn.petals.player.newBluePlayer
import ctmn.petals.tile.TileActor
import ctmn.petals.strings
import ctmn.petals.utils.addCover
import ctmn.petals.utils.removeCover
import ctmn.petals.utils.setPosByCenter
import ctmn.petals.widgets.MapPreview
import ctmn.petals.widgets.addChangeListener
import ctmn.petals.widgets.newIconButton
import ctmn.petals.widgets.newTextButton

/**
 * The reusable half of a game setup screen: the map preview, the eight player slots, and the
 * daytime, season and fog settings.
 *
 * It knows nothing about transports. A host screen edits it and reacts to [onLobbyChanged] by
 * broadcasting the result, which is how the same UI drives both the old peer hosted flow and a
 * dedicated server lobby. The host screen supplies [dialogStage], because slot dialogs are
 * added to a stage rather than to this pane.
 */
class GameSetupPane(val dialogStage: Stage) {

    companion object {
        const val SLOT_COUNT = 8
    }

    val mapPreview = MapPreview()
    val slots = Array<GameSetupSlot>()

    val changeMapButton = newIconButton("change")
    val fogOfWarCheckbox = VisCheckBox(strings.general.fog_of_war)
    val daytimeButton = newTextButton(strings.general.daytime)
    val seasonButton = newTextButton(strings.general.season)

    /**
     * The 300px column that holds the preview, the slots and the settings. A host screen adds
     * its own footer row here, so its buttons line up with the pane instead of floating in a
     * table of their own.
     */
    lateinit var content: VisTable
        private set

    var localPlayer: Player = newBluePlayer
        set(value) {
            field = value
            slots.forEach { it.refreshLocalState() }
        }

    /** Only the host may edit the lobby. */
    var isHost: Boolean = true
        set(value) {
            field = value
            refreshEnabled()
        }

    var gameMode: GameMode = GameMode.CRYSTALS

    /**
     * Whether the slot dialog offers a plain local player. The server flow has no use for it,
     * since its players come from connections.
     */
    var allowLocalPlayer: Boolean = true
        set(value) {
            field = value
        }

    var fogOfWar: Boolean = true
        set(value) {
            field = value
            fogOfWarCheckbox.isChecked = value
        }

    var daytime: PlayStage.DayTime = PlayStage.DayTime.DAY
        set(value) {
            field = value
            daytimeButton.userObject = value
            daytimeButton.setText("${strings.general.daytime}: ${value.name}")
        }

    var season: Season = Season.SUMMER
        set(value) {
            // Setting the same season must not swap the atlas again: a host screen may push
            // the whole lobby state on every update, and rebuilding tile views each time would
            // be wasteful.
            if (field == value && seasonButton.userObject == value) return

            field = value
            seasonButton.userObject = value
            seasonButton.setText("${strings.general.season}: ${value.name}")
            applySeasonAtlas()
        }

    /** The chosen map, or null. */
    val map: MapConverted? get() = mapPreview.map

    /** True when a map is chosen, at least two unlocked slots are filled, and this side hosts. */
    val canStart: Boolean
        get() = occupiedSlotCount >= 2 && map != null && isHost

    private val occupiedSlotCount get() = slots.count { it.player != null && !it.isLocked }

    /** The seated players in slot order, which is the order ids must be assigned in. */
    val players: List<Player> get() = slots.mapNotNull { it.player }

    /** Fired when the host changed something the transport should broadcast. */
    var onLobbyChanged: (() -> Unit)? = null

    /** Fired when a chosen map cannot be played. */
    var onUnplayableMap: ((String) -> Unit)? = null

    init {
        val playersTable = VisTable()
        with(playersTable) {
            for (i in 1..SLOT_COUNT) {
                add(GameSetupSlot()).size(72f, 72f)
                if (i % 4 == 0) row()
            }
        }

        val mainTable = VisTable()
        mainTable.width = 300f
        mainTable.setFillParent(true)
        with(mainTable) {
            add(mapPreview).size(300f).padTop(6f)
            row()
            add(VisScrollPane(VisTable().apply {
                add(playersTable)
                row()
                add(VisTable().apply {
                    add(daytimeButton).colspan(2).width(300f)
                    row()
                    add(seasonButton).colspan(2).width(300f)
                    row()
                    add(fogOfWarCheckbox).colspan(2)
                }).width(300f)
            })).width(300f)
            row()
            add().expandY()
        }.width(300f)

        /**
         * The 300px column holding the preview, slots and settings. A host screen adds its
         * own footer row here so its buttons line up with the rest of the pane.
         */
        content = mainTable

        addActor(mainTable)

        daytime = PlayStage.DayTime.DAY
        season = Season.SUMMER
        fogOfWar = true

        daytimeButton.addChangeListener {
            daytime = when (daytime) {
                PlayStage.DayTime.DAY -> PlayStage.DayTime.EVENING
                PlayStage.DayTime.EVENING -> PlayStage.DayTime.NIGHT
                PlayStage.DayTime.NIGHT -> PlayStage.DayTime.DAY
            }
            onLobbyChanged?.invoke()
        }

        seasonButton.addChangeListener {
            season = when (season) {
                Season.SUMMER -> Season.WINTER
                Season.WINTER -> Season.SUMMER
                else -> Season.SUMMER
            }
            onLobbyChanged?.invoke()
        }

        fogOfWarCheckbox.addChangeListener {
            fogOfWar = fogOfWarCheckbox.isChecked
            onLobbyChanged?.invoke()
        }

        refreshEnabled()
    }

    private fun addActor(actor: Actor) {
        dialogStage.addActor(actor)
    }

    /** Rebuilds tile views, needed after the season swaps the atlas. */
    private fun applySeasonAtlas() {
        assets.tilesAtlas = when (season) {
            Season.WINTER -> assets.tilesWinterAtlas
            else -> assets.tilesSummerAtlas
        }

        map?.actors?.forEach { if (it is TileActor) it.initView() }
    }

    /**
     * Chooses a map and locks the slots the map has no player label for, mirroring the map's
     * own player layout.
     */
    fun setMap(map: MapConverted?) {
        slots.forEach { it.label = null }

        if (map == null) {
            mapPreview.setPreview(null)
            refreshEnabled()
            return
        }

        if (map.mapSave.isOutdatedVersion) {
            onUnplayableMap?.invoke("Outdated map, unplayable")
            mapPreview.setPreview(null)
            refreshEnabled()
            return
        }

        mapPreview.setPreview(map)

        slots.forEach { it.isLocked = true }
        for (label in map.labels) {
            if (label.labelName == "player") {
                val id = label.data["id"].toInt()
                if (id in 0 until slots.size) {
                    slots.get(id).label = label
                    slots.get(id).isLocked = false
                }
            }
        }

        mapPreview.createPlayerMarks()
        slots.forEachIndexed { index, slot ->
            slot.player?.let { mapPreview.changePlayerMark(it, index) }
        }

        refreshEnabled()
    }

    fun setPlayerSlot(player: Player?, slotIndex: Int) {
        if (slotIndex !in 0 until slots.size) throw IllegalArgumentException("slotIndex out of bounds")
        slots.get(slotIndex).player = player
    }

    fun getPlayerSlot(player: Player): GameSetupSlot? = slots.firstOrNull { it.player === player }

    /** Swaps the local player into [slotIndex], keeping whoever was displaced. */
    fun moveLocalPlayerToSlot(slotIndex: Int) {
        if (!isHost) return

        val target = slots.get(slotIndex)
        val previous = getPlayerSlot(localPlayer) ?: throw IllegalStateException("Local player has no slot")
        previous.player = null

        val displaced = target.player
        val displacedIsBot = target.isAI

        target.player = localPlayer
        if (displaced != null) {
            previous.player = displaced
            previous.isAI = displacedIsBot
        }

        onLobbyChanged?.invoke()
    }

    fun removePlayer(player: Player) {
        val slot = getPlayerSlot(player) ?: return
        slot.player = null
        onLobbyChanged?.invoke()
    }

    fun refreshEnabled() {
        changeMapButton.isDisabled = !isHost
        daytimeButton.isDisabled = !isHost
        seasonButton.isDisabled = !isHost
        fogOfWarCheckbox.isDisabled = !isHost
        slots.forEach { it.refreshEnabled() }
    }

    /** The serialisable state of one slot, so both transports agree on the shape. */
    fun slotState(slotIndex: Int): PlayerSlotState = slots.get(slotIndex).slotState()

    /** One player slot: portrait, bot and host marks, and its action dialog. */
    inner class GameSetupSlot : WidgetGroup() {
        val button = newIconButton("player_slot")
        val removePlayerButton = newIconButton("remove_player")

        var label: LabelActor? = null
            set(value) {
                field = value
                player?.let { applyLabelPlayerId(it) }
            }

        val localPlayerMarkImage = VisUI.getSkin().getDrawable("portraits/local_player")
        val hostCrownImage = VisUI.getSkin().getDrawable("portraits/crown")
        val aiPlayerMarkImage = VisUI.getSkin().getDrawable("portraits/ai_player")

        val isLocalHostSlot get() = localPlayer === player && isHost

        var isHostSlot = false
        var isLocalPlayer = false
        var isAI = false

        var isLocked = false
            set(value) {
                color.a = if (value) 0.5f else 1f
                field = value
            }

        var player: Player? = null
            set(value) {
                isAI = false
                button.color.a = 1f

                if (value != null) {
                    updateSpeciesPortrait(value)
                    removePlayerButton.isVisible = value !== localPlayer && isHost
                    mapPreview.changePlayerMark(value, index())
                    applyLabelPlayerId(value)
                } else {
                    button.style.imageUp = null
                    removePlayerButton.isVisible = false
                    mapPreview.changePlayerMark(null, index())
                    mapPreview.map?.labels?.filter { matchesThisSlot(it) }
                        ?.forEach { it.data.removeKey("player_id") }
                }

                isLocalPlayer = value === localPlayer
                field = value
            }

        fun index(): Int = slots.indexOf(this)

        fun refreshLocalState() {
            isLocalPlayer = player === localPlayer
            refreshEnabled()
        }

        fun refreshEnabled() {
            removePlayerButton.isVisible = player != null && player !== localPlayer && isHost
        }

        fun slotState() = PlayerSlotState(player, isLocalHostSlot, isAI)

        init {
            button.addChangeListener { showSlotDialog() }

            setSize(72f, 72f)
            addActor(button)

            removePlayerButton.addChangeListener {
                val seated = player
                if (seated == null || !isHost) return@addChangeListener
                removePlayer(seated)
            }

            removePlayerButton.setPosByCenter(width - 12f, height - 9f)
            removePlayerButton.isVisible = false
            addActor(removePlayerButton)

            addListener(object : InputListener() {
                override fun enter(event: InputEvent?, x: Float, y: Float, pointer: Int, fromActor: Actor?) {
                    super.enter(event, x, y, pointer, fromActor)
                    mapPreview.markButtons.filter { it.userObject == index() }.forEach { it.focusGained() }
                }

                override fun exit(event: InputEvent?, x: Float, y: Float, pointer: Int, toActor: Actor?) {
                    super.exit(event, x, y, pointer, toActor)
                    mapPreview.markButtons.filter { it.userObject == index() }.forEach { it.focusLost() }
                }
            })

            slots.add(this)
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

        private fun matchesThisSlot(candidate: LabelActor): Boolean =
            candidate.labelName == this@GameSetupSlot.label?.labelName &&
                    candidate.data["id"] == index().toString()

        private fun applyLabelPlayerId(seated: Player) {
            mapPreview.map?.labels?.filter { matchesThisSlot(it) }
                ?.forEach { it.data.put("player_id", seated.id.toString()) }
        }

        private fun updateSpeciesPortrait(seated: Player) {
            button.style.imageUp = VisUI.getSkin()
                .getDrawable("portraits/${seated.species + "_" + "abcdefgh"[seated.id - 1]}")
        }

        fun setPlayerSpecies(species: String) {
            val seated = player ?: return
            seated.species = species
            updateSpeciesPortrait(seated)
        }

        private fun showSlotDialog() {
            val slot = this
            dialogStage.addCover()

            val win = VisWindow("${strings.general.player_slot} ${index() + 1}")

            fun close() {
                win.remove()
                dialogStage.removeCover()
                refreshEnabled()
            }

            val moveHereButton = newTextButton(strings.general.move_here).addChangeListener {
                moveLocalPlayerToSlot(index())
                close()
            }

            val addEasyBotButton = newTextButton(strings.general.add_easy_bot).addChangeListener {
                if (slot.player == null) {
                    slot.player = Player(
                        strings.general.player + "ABCDEFGH"[freePlayerId() - 1],
                        freePlayerId(),
                        freePlayerId()
                    ).also { it.species = Species.names.random() }
                    slot.isAI = true
                    onLobbyChanged?.invoke()
                }
                close()
            }

            val addPlayerButton = newTextButton(strings.general.add_player).addChangeListener {
                if ((slot.player == null || slot.isAI) && hasFreeSlot()) {
                    if (slot.player != null) slot.player = null
                    slot.player = Player(
                        strings.general.player + "ABCDEFGH"[freePlayerId() - 1],
                        freePlayerId(),
                        freePlayerId()
                    ).also { it.species = Species.names.random() }
                    onLobbyChanged?.invoke()
                }
                close()
            }

            val makeGoblinButton = newTextButton(strings.general.set_goblin).addChangeListener {
                if (slot.player != null) {
                    slot.setPlayerSpecies(goblin)
                    onLobbyChanged?.invoke()
                }
                close()
            }

            val makeFaerieButton = newTextButton(strings.general.set_faerie).addChangeListener {
                if (slot.player != null) {
                    slot.setPlayerSpecies(fairy)
                    onLobbyChanged?.invoke()
                }
                close()
            }

            val removeButton = newTextButton(strings.general.remove).addChangeListener {
                if (slot.player !== localPlayer && slot.player != null) {
                    removePlayer(slot.player!!)
                }
                close()
            }

            val closeButton = newIconButton("cancel").addChangeListener { close() }

            moveHereButton.isDisabled = !isHost
            addEasyBotButton.isDisabled = !isHost || slot.player != null
            addPlayerButton.isDisabled = !isHost || (slot.player != null && !slot.isAI) || !allowLocalPlayer
            removeButton.isDisabled = !isHost || slot.player === localPlayer

            with(win) {
                setCenterOnAdd(true)
                closeOnEscape()
                add().width(300f)
                row()
                add(moveHereButton)
                row()
                add(addEasyBotButton)
                row()
                if (allowLocalPlayer) {
                    add(addPlayerButton)
                    row()
                }
                if (slot.player != null) {
                    if (slot.player?.species == goblin) {
                        add(makeFaerieButton)
                    } else {
                        add(makeGoblinButton)
                    }
                    row()
                }
                add(removeButton)
                row()
                add(closeButton).padRight(8f).align(Align.right)
                pack()
            }
            dialogStage.addActor(win)
        }
    }

    private fun hasFreeSlot() = slots.firstOrNull { it.player == null } != null

    private fun freePlayerId(): Int = "12345678".toMutableList().apply {
        removeAll { char -> slots.any { it.player?.id == char.toString().toInt() } }
    }.first().toString().toInt()
}
