package ctmn.petals.sim

import com.badlogic.gdx.graphics.Color
import com.badlogic.gdx.scenes.scene2d.Event
import com.badlogic.gdx.scenes.scene2d.EventListener
import ctmn.petals.Config
import ctmn.petals.map.MapConverted
import ctmn.petals.player.Player
import ctmn.petals.playscreen.GameEndCondition
import ctmn.petals.playscreen.GameMode
import ctmn.petals.playscreen.GameType
import ctmn.petals.playscreen.Season
import ctmn.petals.playscreen.TurnManager
import ctmn.petals.playscreen.commands.Command
import ctmn.petals.playscreen.commands.CommandManager
import ctmn.petals.playscreen.events.ActionCompletedEvent
import ctmn.petals.playscreen.events.GameOverEvent
import ctmn.petals.playscreen.events.NextTurnEvent
import ctmn.petals.playscreen.events.UnitMovedEvent
import ctmn.petals.playscreen.listeners.ItemPickUpListener
import ctmn.petals.playscreen.listeners.PinkSlimeLingHealing
import ctmn.petals.playscreen.listeners.TileLifeTimeListener
import ctmn.petals.playscreen.seqactions.SeqAction
import ctmn.petals.bot.BotManager
import ctmn.petals.map.tiles
import ctmn.petals.map.units
import ctmn.petals.map.labels
import ctmn.petals.playscreen.selfName
import ctmn.petals.playstage.getCapturablesOf
import ctmn.petals.tile.TerrainNames
import ctmn.petals.tile.TileActor
import ctmn.petals.tile.TileData
import ctmn.petals.tile.Tiles
import ctmn.petals.tile.isBurning
import ctmn.petals.tile.setPlayerForCapturableTile
import ctmn.petals.unit.Damage
import ctmn.petals.unit.UnitActor
import ctmn.petals.unit.Units
import ctmn.petals.unit.component.TileEffectComponent
import ctmn.petals.unit.dealDamage
import ctmn.petals.unit.isAir
import ctmn.petals.utils.tiledX
import ctmn.petals.utils.tiledY
import ctmn.petals.utils.GameLog
import playScreen.PlayTurnCycleListener
import kotlin.random.Random

/**
 * Authoritative, render free battle state and rules.
 *
 * The dedicated server runs [Battle] (through a headless [SimField]) on its session thread.
 * The client and the local game run a subclass, `PlayScreen`, that adds the scene2d stage,
 * GUI and animations on top of exactly the same state and command pipeline.
 *
 * Nothing here may touch libGDX graphics, assets, a scene2d stage or `Gdx`. Visual work is
 * requested through the `on...View` hooks, which are no-ops on the server.
 */
open class Battle {

    lateinit var field: BattleField
        private set

    var map: MapConverted? = null
    var levelId: String? = null

    val unitsData = Units
    val tilesData = TileData

    var season = Season.SUMMER
    var gameType = GameType.STORY
    var gameMode = GameMode.STORY
    var gameEndCondition: GameEndCondition = ctmn.petals.playscreen.CaptureBases()
    var isGameOver = false

    var gameStateId = 1

    var friendlyFire = false

    var creditsPassiveIncome = 100
    var creditsPassiveIncomeMax = 2000
    var creditsPerBase = 100
    var creditsPerCluster = 100

    /** Optional per turn time limit in seconds. 0 means no limit. Used by the server. */
    var turnTimeLimitSeconds = 0

    /**
     * True while the current battle is driven by a remote server. The online client must not
     * run local bots, the server already runs them and broadcasts their commands.
     */
    var clientControlled = false

    /**
     * False on a client mirror of a server match. Such a client sees a filtered view of the
     * field, so it must not judge the win conditions: the server decides when a match ends.
     */
    var authoritativeGameEnd = true

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

    lateinit var localPlayer: Player

    val hasLocalPlayer: Boolean get() = ::localPlayer.isInitialized

    /**
     * True when the local player is only watching. The flag is set at seating time; owning no
     * capturable tile is the same thing derived from the field, kept as a fallback for flows that
     * never set the flag. The GUI uses this to show a watching state instead of an enemy turn
     * banner for the whole match.
     */
    val isSpectating: Boolean
        get() = hasLocalPlayer && (localPlayer.isSpectator ||
                (this::field.isInitialized && FogOfWar.isSpectator(this.field, localPlayer)))

    private val listeners = ArrayList<EventListener>()

    val turnManager = TurnManager(this)
    val commandManager = CommandManager(this)
    val botManager = BotManager(this)

    private var gameEnding = false

    /** True while a visual action is playing. The headless battle is never busy. */
    open val isActionsBusy: Boolean get() = false

    fun attachField(battleField: BattleField) {
        field = battleField
        onFieldAttached()
    }

    protected open fun onFieldAttached() {
        registerRules()
    }

    private fun registerRules() {
        addEventListener(PlayTurnCycleListener(this))
        addEventListener(EndConditionListener())
        addEventListener(TileLifeTimeListener(field))
        addEventListener(PinkSlimeLingHealing())
        addEventListener(ItemPickUpListener(this))

        // Terrain damage and effects that trigger when a unit finishes its move.
        addEventListener { event ->
            if (event is UnitMovedEvent) {
                val unit = event.unit
                val tile = field.getTile(unit.tiledX, unit.tiledY)
                if (!unit.isAir && tile != null) {
                    when {
                        tile.terrain == TerrainNames.lava -> {
                            unit.dealDamage(Damage.LAVA, battle = this)
                            unit.add(TileEffectComponent(turnManager.currentPlayer.id))
                        }

                        tile.isBurning -> {
                            unit.dealDamage(Damage.BURN, battle = this)
                            unit.add(ctmn.petals.unit.component.BurningComponent(turnManager.currentPlayer.id))
                        }

                        tile.terrain == TerrainNames.chasm -> {
                            unit.dealDamage(Damage.CHASM, battle = this)
                            unit.add(TileEffectComponent(turnManager.currentPlayer.id))
                        }

                        else -> unit.del(TileEffectComponent::class.java)
                    }
                }
            }

            if (event is ActionCompletedEvent) {
                checkGameEndCondition()
            }

            false
        }
    }

    /**
     * Headless tick. The scene2d client overrides this to also tick actions, the camera and
     * the GUI, so it does not call `super.update`.
     */
    open fun update(delta: Float) {
        if (!clientControlled)
            botManager.update(delta)

        commandManager.update(delta)
    }

    fun addEventListener(listener: EventListener) {
        listeners.add(listener)
    }

    fun removeEventListener(listener: EventListener) {
        listeners.remove(listener)
    }

    open fun fireEvent(event: Event) {
        for (listener in listeners.toList()) {
            try {
                listener.handle(event)
            } catch (e: Exception) {
                GameLog.error("Battle", "EventListener failed on ${event::class.simpleName}", e)
            }
        }
    }

    fun checkGameEndCondition() {
        if (isGameOver || gameEnding) return

        // A client that only sees a filtered copy of the field cannot judge the win
        // conditions, so the server stays the only one that ends the match.
        if (!authoritativeGameEnd) return

        for (player in turnManager.players) {
            player.isOutOfGame =
                player.isOutOfGame || gameEndCondition.checkPlayerOutOfGame(player, this) == true
        }

        if (gameEndCondition.check(this)) {
            gameEnding = true
            gameOver()
        }
    }

    fun gameOver() {
        if (isGameOver) return

        gameEnding = true
        isGameOver = true
        fireEvent(GameOverEvent())

        onGameOver()
    }

    open fun onGameOver() {}

    /**
     * Fills the field from a converted map without any view work. Used by the server and
     * headless tests. The scene2d client uses `PlayScreen.setLevel`, which also sorts and
     * decorates the tiles for rendering.
     */
    fun loadMap(map: MapConverted) {
        this.map = map
        levelId = map.mapId

        for (tile in map.tiles) field.addGameActor(tile)
        for (unit in map.units) field.addGameActor(unit)
        for (label in map.labels) field.addGameActor(label)

        normalizeTiles()
    }

    /** Ports the layer and life crystal fixups from `PlayScreen.prepareLevel`, view free. */
    private fun normalizeTiles() {
        val tiles = ArrayList(field.allTiles)
        val samePos = ArrayList<TileActor>()

        for (x in 0 until field.tiledWidth) {
            for (y in 0 until field.tiledHeight) {
                samePos.clear()
                tiles.forEach { if (it.tiledX == x && it.tiledY == y) samePos.add(it) }
                samePos.sortByDescending { it.layer }
                if (samePos.isEmpty()) continue

                if (samePos.size == 1) {
                    samePos.first().layer = 1
                    continue
                }

                if (samePos.firstOrNull { it.layer == 2 } == null) continue

                samePos.filter { it.layer < 3 }.forEach { it.layer -= 1 }
            }
        }

        if (gameType != GameType.STORY) {
            for (tile in tiles.filter { it.selfName == Tiles.LIFE_CRYSTAL }) {
                field.removeGameActor(tile)
                field.addGameActor(TileActor(TileData.get(Tiles.CRYSTAL)!!, tile.layer, tile.tiledX, tile.tiledY))
            }
        }
    }

    /**
     * Gives every `player` label its player id, the same defaulting the client does through
     * `PlayScreen.setDefaultPlayerIdToLabels`. Without it no base has an owner and the end
     * condition immediately considers everyone defeated.
     */
    fun setupDefaultPlayerIds() {
        for (label in field.labelActors.filter { it.selfName == "player" }) {
            if (label.data["player_id"] != null) continue

            val id = label.data["id"]?.toIntOrNull() ?: continue
            label.data.put("player_id", (id + 1).toString())
        }
    }

    /**
     * Gives each map player place to whoever sits in the matching lobby slot, so the host's
     * arrangement decides the map while a player id only decides their colour. A place with
     * nobody in its slot is left unowned, which is how a spectator sits the game out.
     */
    fun assignMapPlacesBySlot(playerIdBySlot: Map<Int, Int>) {
        for (label in field.labelActors.filter { it.selfName == "player" }) {
            label.data.removeKey("player_id")

            val slot = label.data["id"]?.toIntOrNull() ?: continue
            val playerId = playerIdBySlot[slot] ?: continue

            // Only a seat the player list actually knows about may own a place.
            if (turnManager.getPlayerById(playerId) == null) continue

            label.data.put("player_id", playerId.toString())
        }
    }

    /** Assigns map player labels to bases and spawns, ported from `PlayScreen`. */
    fun setupSpawnPoints() {        val map = this.map ?: return

        for (label in field.labelActors.filter { it.selfName == "player" }) {
            val playerId = label.data["player_id"]?.toInt()

            val base = map.playerBases.firstOrNull {
                it.tiledX == label.tiledX && it.tiledY == label.tiledY
            } ?: continue

            if (playerId != null) {
                setPlayerForCapturableTile(
                    base,
                    playerId,
                    turnManager.getPlayerById(playerId)?.species
                )
            } else {
                setPlayerForCapturableTile(base, -1)
            }

            if (gameType != GameType.STORY) {
                if (base.selfName == Tiles.LIFE_CRYSTAL) {
                    field.removeGameActor(base)
                    val crystal = TileActor(TileData.get(Tiles.CRYSTAL)!!, base.layer, base.tiledX, base.tiledY)
                    field.addGameActor(crystal)
                }
            }
        }
    }

    /** Gives the first player the base income and announces the first turn. */
    fun startFirstTurn() {
        val currentPlayer = turnManager.currentPlayer
        for (base in field.getCapturablesOf(currentPlayer))
            currentPlayer.credits += creditsPerBase

        fireEvent(NextTurnEvent(currentPlayer, currentPlayer))
    }

    fun queueCommand(command: Command, playerId: Int? = null): Boolean =
        commandManager.queueCommand(command, playerId)

    // View hooks. No-ops on the server, overridden by the scene2d client.

    open fun showFloatingLabel(text: String, color: Color? = null, x: Float = 0f, y: Float = 0f) {}

    open fun showFloatingIconLabel(text: String, icon: String, x: Float, y: Float) {}

    open fun showHealthChange(unit: UnitActor, amount: Int) {}

    open fun onUnitDiedView(unit: UnitActor) {}

    open fun onUnitMovedView(unit: UnitActor, fromX: Int, fromY: Int) {}

    open fun onTileOwnerChangedView(tile: TileActor) {}

    open fun onUnitSummonedView(unit: UnitActor) {}

    open fun queueSeqAction(action: SeqAction) {}

    open fun queueSeqActionFirst(action: SeqAction) {}

    /** Adds a purely cosmetic scene2d actor (effect, missile, label) to the client stage. */
    open fun showEffect(effect: com.badlogic.gdx.scenes.scene2d.Actor) {}

    /**
     * Ability visuals are plain scene2d actors rather than sequence actions, but they are
     * queued through the same call. Headless they are dropped by [showEffect].
     */
    open fun queueSeqAction(effect: com.badlogic.gdx.scenes.scene2d.Actor) {
        showEffect(effect)
    }

    private inner class EndConditionListener : EventListener {
        override fun handle(event: Event): Boolean {
            if (event is NextTurnEvent && event.previousPlayer != event.nextPlayer) {
                checkGameEndCondition()
            }

            return false
        }
    }
}
