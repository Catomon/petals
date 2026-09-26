package ctmn.petals.bot.v2

import com.badlogic.gdx.utils.Array
import ctmn.petals.bot.Bot
import ctmn.petals.bot.v2.actions.AbilityAction
import ctmn.petals.bot.v2.actions.AttackAction
import ctmn.petals.bot.v2.actions.BotAction
import ctmn.petals.bot.v2.actions.BuildBaseAction
import ctmn.petals.bot.v2.actions.BuildBuildingAction
import ctmn.petals.bot.v2.actions.BuyUnitAction
import ctmn.petals.bot.v2.actions.CaptureAction
import ctmn.petals.bot.v2.actions.DestroyAction
import ctmn.petals.bot.v2.actions.MoveAction
import ctmn.petals.player.Player
import ctmn.petals.player.Species
import ctmn.petals.sim.Battle
import ctmn.petals.sim.BattleField
import ctmn.petals.unit.UnitActor
import ctmn.petals.unit.canAttackNow
import ctmn.petals.unit.canDestroy
import ctmn.petals.unit.component.RoamingPosition
import ctmn.petals.unit.tiledX
import ctmn.petals.unit.tiledY
import ctmn.petals.utils.GameLog
import kotlin.random.Random

/**
 * A complete, difficulty configurable bot that plays with the same rules as a human:
 * it buys, builds, captures, fights, uses abilities and destroys enemy economy.
 *
 * Economy actions are enabled automatically based on what the map offers, so the same bot
 * also works on campaign maps without buildings or resources.
 */
class AiBot(
    player: Player,
    battle: Battle,
    val settings: AiBotSettings = AiBotSettings(),
) : Bot(player, battle) {

    val difficulty get() = settings.difficulty
    val context = BotContext(this)
    val field get() = battle.field

    /**
     * Whether the battle uses fog of war. Fog is a client rendering concern, so the headless
     * server always leaves it false. The scene2d client can set it when it creates the bot.
     */
    var fogOfWar: Boolean = false

    /**
     * AI-local RNG. Evaluation never touches the game RNG, so bot turns cannot change
     * combat rolls or disturb multiplayer determinism.
     */
    val random = Random(BOT_SEED + player.id * 7919)

    /** The roster the bot may buy. Campaign levels can override it with custom creatures. */
    val speciesUnits: Array<UnitActor> = settings.speciesUnits ?: Array<UnitActor>().apply {
        runCatching { Species.getSpeciesUnits(player.species) }
            .getOrNull()
            ?.forEach { add(it.unitActor) }
    }

    var capabilities: BotCapabilities =
        BotCapabilities.detect(player, battle, speciesUnits, settings.forceCombatOnly)
        private set

    private var elapsed = 0f
    private var idle = 0f
    private val attempts = HashMap<String, Int>()
    private val blocked = HashSet<String>()

    /** Actions successfully issued this turn, for the balance log. */
    private val actionsThisTurn = HashMap<String, Int>()

    /**
     * Starting position for scripted roaming units. Stored on the unit itself so several
     * creatures of the same type each keep their own patrol anchor (map units have no name).
     */
    fun roamingAnchor(unit: UnitActor): Pair<Int, Int> {
        val existing = unit.get(RoamingPosition::class.java)
        if (existing != null) return existing.tiledX to existing.tiledY

        unit.add(RoamingPosition(unit.tiledX, unit.tiledY))
        return unit.tiledX to unit.tiledY
    }

    override fun levelCreated(field: BattleField) {
        super.levelCreated(field)
        BotLog.resetMatch()
        refreshCapabilities()
    }

    override fun onStart() {
        super.onStart()
        elapsed = 0f
        idle = 0f
        attempts.clear()
        blocked.clear()
        actionsThisTurn.clear()
        context.refresh()
        refreshCapabilities()

        BotLog.mapSummaryIfNeeded(battle)
        BotLog.turnStart(this)

        // INSANE is the only difficulty allowed to cheat: it gets bonus credits each turn.
        if (difficulty.incomeBonus > 0) {
            player.credits += difficulty.incomeBonus
            BotLog.incomeBonus(this, difficulty.incomeBonus)
        }
    }

    override fun onEnd() {
        super.onEnd()
        BotLog.turnEnd(this, actionsThisTurn)
        attempts.clear()
        blocked.clear()
    }

    private fun refreshCapabilities() {
        capabilities = BotCapabilities.detect(player, battle, speciesUnits, settings.forceCombatOnly)
    }

    override fun update(delta: Float) {
        if (isDone) return

        if (battle.isActionsBusy) {
            elapsed = 0f
            return
        }

        elapsed += delta
        if (elapsed < difficulty.actionDelay) return
        elapsed = 0f

        val action = pickAction()
        if (action == null) {
            idle += difficulty.actionDelay
            if (idle >= difficulty.endTurnDelay) {
                BotLog.noActions(this)
                done()
            }
            return
        }

        idle = 0f
        val signature = action.signature()
        if (blocked.contains(signature)) return

        val success = try {
            action.execute()
        } catch (t: Throwable) {
            GameLog.error("AiBot", "execute failed for ${action.signature()}", t)
            false
        }
        if (success) {
            val kind = action.commandKind()
            actionsThisTurn[kind] = (actionsThisTurn[kind] ?: 0) + 1
        } else {
            val count = (attempts[signature] ?: 0) + 1
            attempts[signature] = count
            if (count >= 2) blocked.add(signature)
        }
    }

    private fun pickAction(): BotAction? {
        context.refresh()
        refreshCapabilities()

        // Cheap, high value actions first. Movement (which computes movement grids) is only
        // considered when nothing else is worth doing.
        val tactical = ArrayList<BotAction>()

        for (unit in context.units) {
            if (unit.canAttackNow()) tactical.add(AttackAction(this, unit))
        }
        if (difficulty.useAbilities) {
            for (unit in context.units) tactical.add(AbilityAction(this, unit))
        }
        if (capabilities.canBuy) {
            for (base in context.freeOwnedBases()) tactical.add(BuyUnitAction(this, base))
        }
        if (capabilities.hasWorkers) {
            for (worker in context.workers) {
                tactical.add(CaptureAction(this, worker))
                tactical.add(BuildBaseAction(this, worker))
                tactical.add(BuildBuildingAction(this, worker))
            }
        }
        if (capabilities.canDestroy && settings.attackBuildings) {
            for (unit in context.units) {
                if (unit.canDestroy()) tactical.add(DestroyAction(this, unit))
            }
        }

        bestOf(tactical)?.let { return it }

        val movement = ArrayList<BotAction>()
        for (unit in context.units) movement.add(MoveAction(this, unit))
        return bestOf(movement)
    }

    /**
     * Picks the highest scoring action. Lower skill means the bot more often misreads the
     * board and plays a runner-up instead of the best move it can see.
     */
    private fun bestOf(actions: List<BotAction>): BotAction? {
        val scored = ArrayList<Pair<BotAction, Int>>()
        for (action in actions) {
            if (blocked.contains(action.signature())) continue
            val score = try {
                action.evaluate()
            } catch (t: Throwable) {
                GameLog.error("AiBot", "evaluate failed for ${action.signature()}", t)
                0
            }
            if (score > 0) scored.add(action to score)
        }
        if (scored.isEmpty()) return null

        val ranked = scored.sortedByDescending { it.second }

        val mistakeChance = MISTAKE_RATE * (1f - difficulty.skill)
        if (ranked.size > 1 && random.nextFloat() < mistakeChance) {
            val pool = ranked.drop(1).take(MISTAKE_POOL).map { it.first }
            return pool.random(random)
        }

        return ranked.first().first
    }

    private companion object {
        const val BOT_SEED = 0x5EED

        /** Chance multiplier for picking a runner-up action at zero skill. */
        const val MISTAKE_RATE = 0.5f

        /** How many runner-up actions can be picked by mistake. */
        const val MISTAKE_POOL = 3
    }
}
