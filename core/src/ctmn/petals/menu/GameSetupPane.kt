package ctmn.petals.menu

import com.badlogic.gdx.Gdx
import com.badlogic.gdx.graphics.g2d.Batch
import com.badlogic.gdx.scenes.scene2d.Actor
import com.badlogic.gdx.scenes.scene2d.Event
import com.badlogic.gdx.scenes.scene2d.EventListener
import com.badlogic.gdx.scenes.scene2d.InputEvent
import com.badlogic.gdx.scenes.scene2d.InputListener
import com.badlogic.gdx.scenes.scene2d.Stage
import com.badlogic.gdx.scenes.scene2d.Touchable
import com.badlogic.gdx.scenes.scene2d.ui.Stack
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

        /** Bot difficulties offered per slot, weakest first. */
        val BOT_DIFFICULTIES = listOf("easy", "normal", "hard", "insane")
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

    /**
     * The lobby's host. The crown follows this player from slot to slot, instead of sitting on
     * whichever slot they first sat in and staying there when they move.
     */
    var hostPlayer: Player? = null

    /**
     * A row between the slots and the settings, for a screen that needs one action there. Empty
     * by default, and an empty table takes no room.
     */
    val aboveSettings = VisTable()

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

    /**
     * The map places, as slot indices: one per "player" label the map carries. A seat outside
     * this list has no place on the map and only watches.
     */
    val placeIndices: List<Int>
        get() = map?.labels
            ?.filter { it.labelName == "player" }
            ?.mapNotNull { it.data["id"]?.toIntOrNull() }
            ?.filter { it in 0 until slots.size }
            ?: emptyList()

    /**
     * Every place the map offers has somebody in it. A match must not start with a place empty,
     * because an unowned base is free for anyone to walk into; a lobby whose places are not
     * filled waits instead of starting.
     */
    val allPlacesFilled: Boolean
        get() {
            val places = placeIndices
            return places.isNotEmpty() && places.all { slots.get(it).player != null }
        }

    /** True when this seat has no place on the map, so its player can only watch. */
    fun isSeatWithoutPlace(player: Player): Boolean = getPlayerSlot(player)?.isLocked == true

    /**
     * The chosen map has somebody in every place, there is an opponent, and this side hosts. A
     * seat past the map's places does not count as an opponent: that player is watching.
     */
    val canStart: Boolean
        get() = map != null && isHost && occupiedSlotCount >= 2 && allPlacesFilled

    /**
     * Every occupied slot, including the locked ones the map has no place for. Somebody sitting
     * past the map's player places only watches, but the match is still a real match once a
     * second player or bot is in it, so this is the count a lobby waits on.
     */
    val seatedCount get() = slots.count { it.player != null }

    private val occupiedSlotCount get() = slots.count { it.player != null && !it.isLocked }

    /** The seated players in slot order, which is the order ids must be assigned in. */
    val players: List<Player> get() = slots.mapNotNull { it.player }

    /** The bot seats, in slot order, each with its own difficulty. */
    val bots: List<Pair<String, Player>>
        get() = slots.filter { it.isAI && it.player != null }.map { it.botDifficulty to it.player!! }

    /**
     * Fired when the local player moved themselves to another seat. A host's screen sends its
     * whole arrangement through [onLobbyChanged]; everybody else's seat is theirs to ask for.
     */
    var onLocalSeatMoved: ((Int) -> Unit)? = null

    /**
     * Fired when the host asks to remove a player from the lobby, [banned] true meaning they may
     * not come back. A transport that owns the lobby (the server) removes the member; when this is
     * null the seat is only cleared on this screen, which is all same screen play needs.
     */
    var onPlayerKicked: ((Player, Boolean) -> Unit)? = null

    /**
     * Whether the local player may take another seat. True where the transport carries the move,
     * which is the host's own screen and a dedicated server lobby. A peer hosted client cannot:
     * its host owns the arrangement and would only put them back.
     */
    var allowLocalSeatMove: Boolean = true

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
            // The change map button sits over the preview's bottom right corner, stacked in the
            // same cell. A separate overlay table was tried before, but it positions by faith
            // and landed off the preview on any stage that lays the column out differently.
            // Children only touchable, so clicks anywhere else reach the preview itself.
            add(Stack().apply {
                add(mapPreview)
                add(VisTable().apply {
                    touchable = Touchable.childrenOnly
                    add(changeMapButton).expand().bottom().right()
                })
            }).size(300f, 260f).padTop(6f)
            row()

            // Slots and settings go straight into the column. They are a fixed height, so a scroll
            // pane around them only added a way for the bottom of the screen to get squeezed: the
            // pane is taller than the viewport and the scroll pane swallowed the difference.
            add(playersTable).width(300f)
            row()
            add(aboveSettings).width(300f)
            row()
            add(VisTable().apply {
                add(daytimeButton).colspan(2).width(300f)
                row()
                add(seasonButton).colspan(2).width(300f)
                row()
                add(fogOfWarCheckbox).colspan(2)
            }).width(300f)
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
        onLobbyChanged?.invoke()
    }

    fun getPlayerSlot(player: Player): GameSetupSlot? = slots.firstOrNull { it.player === player }

    /** Swaps the local player into [slotIndex], keeping whoever was displaced. */
    fun moveLocalPlayerToSlot(slotIndex: Int) {
        if (!isHost && !allowLocalSeatMove) return

        val target = slots.get(slotIndex)

        // The host arranges anybody anywhere. Anybody else may take a free seat or a bot's, but a
        // seat another player is sitting in is not theirs to take. The server refuses it too.
        if (!isHost && target.player != null && !target.isAI) return

        // The local player may have no seat yet, for example on a fresh server lobby screen,
        // so this must not insist on one.
        val previous = getPlayerSlot(localPlayer)
        val displaced = target.player
        val displacedIsBot = target.isAI
        val displacedDifficulty = target.botDifficulty

        // With no seat of their own there is nowhere for the displaced player to go, and the move
        // would silently drop them.
        if (previous == null && displaced != null) return

        previous?.player = null

        target.player = localPlayer

        // The seat holds a person now. The bot marks and the difficulty belong to whoever was in
        // it, and they travel with them to the seat the local player left.
        target.isAI = false
        target.botDifficulty = "normal"

        if (displaced != null && previous != null) {
            previous.player = displaced
            previous.isAI = displacedIsBot
            previous.botDifficulty = displacedDifficulty
        }

        onLobbyChanged?.invoke()
        onLocalSeatMoved?.invoke(slotIndex)
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

        // Not isDisabled: a disabled VisCheckBox loses its tick, so a joiner would see fog as off
        // while it is on. Touchable keeps the real setting visible and still out of their reach.
        fogOfWarCheckbox.touchable = if (isHost) Touchable.enabled else Touchable.disabled

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

        /**
         * The local player's own seat mark, worked out when it is drawn. A stored flag went stale
         * whenever a seat changed hands without going through this slot's own setter.
         */
        val isMe get() = player != null && player === localPlayer

        var isAI = false

        /** Difficulty for this slot's bot, see [ctmn.petals.bot.v2.Difficulty]. */
        var botDifficulty: String = "normal"

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

                field = value
            }

        fun index(): Int = slots.indexOf(this)

        fun refreshLocalState() {
            refreshEnabled()
        }

        fun refreshEnabled() {
            removePlayerButton.isVisible =
                isHost && player != null && player !== localPlayer
        }

        fun slotState() = PlayerSlotState(player, isLocalHostSlot, isAI)

        init {
            button.addChangeListener { showSlotDialog() }

            setSize(72f, 72f)
            addActor(button)

            removePlayerButton.addChangeListener {
                val seated = player ?: return@addChangeListener
                if (!isHost) return@addChangeListener

                // A member is removed for real through the transport; a bot is not a member, it
                // goes when the next arrangement leaves it out.
                if (isAI || onPlayerKicked == null) removePlayer(seated)
                else onPlayerKicked!!.invoke(seated, false)
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

            if (isMe) {
                localPlayerMarkImage.draw(batch, x, y, localPlayerMarkImage.minWidth, localPlayerMarkImage.minHeight)
            }

            // The crown belongs to the host, so it moves with them rather than staying on the slot
            // they happened to start in.
            if (isLocalHostSlot || (player != null && player === hostPlayer)) {
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

            if (!isHost && !allowLocalSeatMove) return

            dialogStage.addCover()

            val win = VisWindow("${strings.general.player_slot} ${index() + 1}")

            // The cover must go away however the window closes, otherwise it keeps swallowing
            // clicks. Closing it by escape or by tapping outside never runs our own listeners.
            win.addListener(object : EventListener {
                override fun handle(event: Event?): Boolean {
                    if (event is InputEvent && event.type == InputEvent.Type.exit) {
                        dialogStage.removeCover()
                    }
                    return false
                }
            })

            fun close() {
                win.remove()
                dialogStage.removeCover()
                refreshEnabled()
            }

            val moveHereButton = newTextButton(strings.general.move_here).addChangeListener {
                moveLocalPlayerToSlot(index())
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

            // One button per bot difficulty, so a lobby can mix them.
            val addBotButtons = BOT_DIFFICULTIES.map { difficulty ->
                newTextButton("Bot: $difficulty").addChangeListener {
                    if (slot.player == null) {
                        slot.player = Player(
                            strings.general.player + "ABCDEFGH"[freePlayerId() - 1],
                            freePlayerId(),
                            freePlayerId()
                        ).also { it.species = Species.names.random() }
                        slot.isAI = true
                        slot.botDifficulty = difficulty
                        onLobbyChanged?.invoke()
                    }
                    close()
                }
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
                val target = slot.player
                if (target !== localPlayer && target != null) {
                    // A member is removed for real when a transport carries it; a bot is not a
                    // member, it goes when the next arrangement leaves it out.
                    if (slot.isAI || onPlayerKicked == null) removePlayer(target)
                    else onPlayerKicked!!.invoke(target, false)
                }
                close()
            }

            // A ban is a removal that also keeps them from rejoining, so it only means something
            // where a server holds the lobby, and only against another person.
            val isOtherHuman = slot.player != null && !slot.isAI && slot.player !== localPlayer
            val canBan = isHost && onPlayerKicked != null && isOtherHuman
            val banButton = newTextButton("Ban").addChangeListener {
                val target = slot.player
                if (canBan && target != null) onPlayerKicked!!.invoke(target, true)
                close()
            }

            val closeButton = newIconButton("cancel").addChangeListener { close() }

            // The host arranges anybody anywhere. Anybody else may take a free seat or a bot's,
            // never a seat another player is sitting in.
            moveHereButton.isDisabled = when {
                isHost -> false
                !allowLocalSeatMove -> true
                slot.player === localPlayer -> true
                isOtherHuman -> true
                else -> false
            }
            addPlayerButton.isDisabled = !isHost || (slot.player != null && !slot.isAI) || !allowLocalPlayer
            removeButton.isDisabled = !isHost || slot.player === localPlayer
            banButton.isVisible = canBan
            banButton.isDisabled = !canBan
            addBotButtons.forEach { it.isDisabled = !isHost || slot.player != null }

            with(win) {
                setCenterOnAdd(true)
                closeOnEscape()
                add().width(300f)
                row()
                add(moveHereButton)
                row()

                // Everything below taking a seat is the host's to change. A player moving
                // themselves sees one button, which is the whole of what they may do here.
                if (isHost) {
                    for (botButton in addBotButtons) {
                        add(botButton)
                        row()
                    }
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
                    if (banButton.isVisible) {
                        add(banButton)
                        row()
                    }
                }
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
