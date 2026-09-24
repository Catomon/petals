package ctmn.petals.bot.v2

import com.badlogic.gdx.Gdx
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
import ctmn.petals.playscreen.PlayScreen
import ctmn.petals.playscreen.selfName
import ctmn.petals.playstage.PlayStage
import ctmn.petals.unit.UnitActor
import ctmn.petals.unit.canAttackNow
import ctmn.petals.unit.canDestroy
import ctmn.petals.unit.tiledX
import ctmn.petals.unit.tiledY

/**
 * A complete, difficulty configurable bot that plays with the same rules as a human:
 * it buys, builds, captures, fights, uses abilities and destroys enemy economy.
 *
 * Economy actions are enabled automatically based on what the map offers, so the same bot
 * also works on campaign maps without buildings or resources.
 */
class AiBot(
    player: Player,
    playScreen: PlayScreen,
    val settings: AiBotSettings = AiBotSettings(),
) : Bot(player, playScreen) {

    val difficulty get() = settings.difficulty
    val context = BotContext(this)
    val playStage get() = playScreen.playStage

    /** The roster the bot may buy. Campaign levels can override it with custom creatures. */
    val speciesUnits: Array<UnitActor> = settings.speciesUnits ?: Array<UnitActor>().apply {
        runCatching { Species.getSpeciesUnits(player.species) }
            .getOrNull()
            ?.forEach { add(it.unitActor) }
    }

    var capabilities: BotCapabilities =
        BotCapabilities.detect(player, playScreen, speciesUnits, settings.forceCombatOnly)
        private set

    private var elapsed = 0f
    private var idle = 0f
    private val attempts = HashMap<String, Int>()
    private val blocked = HashSet<String>()

    /** Starting position for scripted roaming units, keyed by unit name. */
    private val roamingPositions = HashMap<String, Pair<Int, Int>>()

    fun roamingAnchor(unit: UnitActor): Pair<Int, Int> {
        val key = unit.name ?: unit.selfName
        return roamingPositions.getOrPut(key) { unit.tiledX to unit.tiledY }
    }

    override fun levelCreated(playStage: PlayStage) {
        super.levelCreated(playStage)
        refreshCapabilities()
    }

    override fun onStart() {
        super.onStart()
        elapsed = 0f
        idle = 0f
        attempts.clear()
        blocked.clear()
        context.refresh()
        refreshCapabilities()
    }

    override fun onEnd() {
        super.onEnd()
        attempts.clear()
        blocked.clear()
    }

    private fun refreshCapabilities() {
        capabilities = BotCapabilities.detect(player, playScreen, speciesUnits, settings.forceCombatOnly)
    }

    override fun update(delta: Float) {
        if (isDone) return

        if (playScreen.actionManager.hasActions) {
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
                Gdx.app.debug("AiBot", "${player.name}: no actions, ending turn.")
                done()
            }
            return
        }

        idle = 0f
        val signature = action.signature()
        if (blocked.contains(signature)) return

        val success = runCatching { action.execute() }.getOrDefault(false)
        if (!success) {
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
        if (capabilities.canDestroy) {
            for (unit in context.units) {
                if (unit.canDestroy()) tactical.add(DestroyAction(this, unit))
            }
        }

        bestOf(tactical)?.let { return it }

        val movement = ArrayList<BotAction>()
        for (unit in context.units) movement.add(MoveAction(this, unit))
        return bestOf(movement)
    }

    private fun bestOf(actions: List<BotAction>): BotAction? {
        var best: BotAction? = null
        var bestScore = 0
        for (action in actions) {
            if (blocked.contains(action.signature())) continue
            val score = runCatching { action.evaluate() }.getOrDefault(0)
            if (score > bestScore) {
                bestScore = score
                best = action
            }
        }
        return best
    }
}
