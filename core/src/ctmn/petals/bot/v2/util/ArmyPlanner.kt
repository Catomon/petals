package ctmn.petals.bot.v2.util

import com.badlogic.gdx.utils.Array
import ctmn.petals.Config
import ctmn.petals.bot.v2.AiBot
import ctmn.petals.bot.v2.Difficulty
import ctmn.petals.player.Species
import ctmn.petals.playscreen.selfName
import ctmn.petals.playstage.getCapturablesOf
import ctmn.petals.playstage.getTiles
import ctmn.petals.playstage.tiledDst
import ctmn.petals.tile.TileActor
import ctmn.petals.tile.isWaterBase
import ctmn.petals.tile.components.BuildingComponent
import ctmn.petals.unit.TerrainPropsPack
import ctmn.petals.unit.UnitActor
import ctmn.petals.unit.canAttackAir
import ctmn.petals.unit.cAttack
import ctmn.petals.unit.cShop
import ctmn.petals.unit.component.MoveAfterAttackComponent
import ctmn.petals.unit.defense
import ctmn.petals.unit.isAir
import ctmn.petals.unit.isWater
import ctmn.petals.unit.isWorker
import ctmn.petals.unit.tiledX
import ctmn.petals.unit.tiledY
import ctmn.petals.utils.getSurroundingTiles
import kotlin.math.ceil
import kotlin.math.roundToInt
import kotlin.math.sqrt

/**
 * Army and economy decisions: what to buy, what to build and when to expand.
 *
 * The plan is context driven instead of a fixed per-difficulty roster. Map area, economy,
 * production capacity, game phase and the enemy army all contribute, and
 * [Difficulty.ambition] scales how far the bot is willing to go.
 */
object ArmyPlanner {

    /** Map area, in tiles, that counts as "normal" (20x20). */
    private const val MAP_AREA_REFERENCE = 400f

    /** How close an enemy has to be to a base before the bot considers walls. */
    private const val DEFENSE_THREAT_RANGE = 6

    /** Terrain is "strong" if it gives at least this much combat bonus. */
    private const val STRONG_TERRAIN_BONUS = 8

    /** Buildings the bot keeps as static defense at most. */
    private const val MAX_DEFENSIVE_BUILDINGS = 2

    /** Hard cap on owned bases, regardless of ambition. */
    private const val MAX_BASES = 5

    private enum class Phase { EARLY, MID, LATE }

    private val BUY_ORDER = listOf(
        UnitRole.WORKER,
        UnitRole.SCOUT,
        UnitRole.HEALER,
        UnitRole.RANGED,
        UnitRole.ARTILLERY,
        UnitRole.SKIRMISHER,
        UnitRole.MELEE,
        UnitRole.AIR,
        UnitRole.WATER,
    )

    fun isUnlocked(bot: AiBot, unit: UnitActor): Boolean =
        requiredBuildings(bot, unit.selfName).all { bot.context.ownsBuilding(it) }

    fun requiredBuildings(bot: AiBot, unitName: String): List<String> =
        runCatching {
            Species.getSpeciesUnits(bot.player.species)
                .firstOrNull { it.unitActor.selfName == unitName }
                ?.requiredBuildings ?: emptyList()
        }.getOrDefault(emptyList())

    /** True if this unit still fills a gap in the current army composition. */
    fun isUnitWanted(bot: AiBot, unit: UnitActor): Boolean =
        isUnitWanted(bot, unit, bot.speciesUnits.toList())

    private fun isUnitWanted(bot: AiBot, unit: UnitActor, roster: List<UnitActor>): Boolean {
        val role = UnitRoles.of(unit)
        val targets = roleTargets(bot, roster)
        val target = targets[role] ?: 0
        if (target <= 0) return false

        val count = bot.context.units.count { UnitRoles.of(it) == role }
        if (count < target) return true

        // At target: keep progressing through the tech tree when the unit is a real upgrade
        // over what we can field now, but only once the economy supports it.
        if (!chasesUpgrades(bot)) return false
        val current = roster
            .filter { UnitRoles.of(it) == role && it.cShop != null && isUnlocked(bot, it) }
            .maxByOrNull { scoreUnit(bot, it) } ?: return true
        return scoreUnit(bot, unit) > scoreUnit(bot, current)
    }

    /** Read-only role target snapshot, used by balance logging. */
    fun roleTargets(bot: AiBot): Map<UnitRole, Int> = roleTargets(bot, bot.speciesUnits.toList())

    /** Credits the bot is saving for its next unlock, used by balance logging. */
    fun nextTechReserve(bot: AiBot): Int = nextTechReserve(bot, bot.speciesUnits.toList())

    /** A scored unit choice, with the runners-up kept for balance logging. */
    class BuyChoice(
        val unit: UnitActor,
        val role: UnitRole,
        val score: Float,
        val alternatives: List<Pair<UnitActor, Float>>,
        val emergencyAir: Boolean = false,
    )

    /** Name of a unit to buy at [base], or null if nothing is worth buying there. */
    fun chooseUnitToBuy(bot: AiBot, base: TileActor, speciesUnits: Array<UnitActor>): String? =
        chooseUnitToBuyChoice(bot, base, speciesUnits)?.unit?.selfName

    /** The full buy decision, including scores, so balance logging can explain it. */
    fun chooseUnitToBuyChoice(bot: AiBot, base: TileActor, speciesUnits: Array<UnitActor>): BuyChoice? {
        // Materialize the gdx Array once: its iterator cannot be used nested.
        val units = speciesUnits.toList()
        val ctx = bot.context
        val player = ctx.player
        if (player.credits <= 0) return null

        val baseIsWater = base.isWaterBase
        val reserve = nextTechReserve(bot, units)

        val candidates = units.filter { unit ->
            val shop = unit.cShop ?: return@filter false
            player.credits >= shop.price &&
                isUnlocked(bot, unit) &&
                if (baseIsWater) unit.isWater else !unit.isWater
        }
        if (candidates.isEmpty()) return null

        val targets = roleTargets(bot, units)
        fun need(role: UnitRole): Int =
            (targets[role] ?: 0) - ctx.units.count { UnitRoles.of(it) == role }

        fun affordable(unit: UnitActor): Boolean {
            val price = unit.cShop?.price ?: return false
            // The first worker is always worth it: without one there is no economy at all.
            if (unit.isWorker && ctx.workers.isEmpty()) return true
            // Do not sit on a reserve while the army does not exist yet.
            if (ctx.combatUnits.isEmpty() && ctx.enemies.isNotEmpty()) return true
            return player.credits - price >= reserve
        }

        // Emergency anti-air: never let enemy fliers sit unanswered.
        val ownAirDefense = ctx.units.count { it.canAttackAir }
        val enemyAir = ctx.enemies.count { it.isAir }
        if (enemyAir > ownAirDefense) {
            val pool = candidates.filter {
                it.canAttackAir && UnitRoles.of(it) != UnitRole.SCOUT && affordable(it)
            }
            chooseBest(bot, pool, emergencyAir = true)?.let { return it }
        }

        // If hostiles are already at the gates and there is no army, buy a defender before
        // spending more on workers.
        val defendFirst = ctx.combatUnits.isEmpty() && ctx.enemies.isNotEmpty() &&
            ctx.ownBases.any { ownBase ->
                ctx.enemies.any { tiledDst(it.tiledX, it.tiledY, ownBase.tiledX, ownBase.tiledY) <= DEFENSE_THREAT_RANGE }
            }
        val order = if (defendFirst) BUY_ORDER.filterNot { it == UnitRole.WORKER } + UnitRole.WORKER else BUY_ORDER

        for (role in order) {
            if (need(role) <= 0) continue
            val pool = candidates.filter { UnitRoles.of(it) == role && affordable(it) }
            chooseBest(bot, pool)?.let { return it }
        }

        return null
    }

    private fun chooseBest(bot: AiBot, pool: List<UnitActor>, emergencyAir: Boolean = false): BuyChoice? {
        if (pool.isEmpty()) return null
        val scored = pool.map { it to scoreUnit(bot, it) }.sortedByDescending { it.second }
        val best = scored.first()
        return BuyChoice(
            unit = best.first,
            role = UnitRoles.of(best.first),
            score = best.second,
            alternatives = scored,
            emergencyAir = emergencyAir,
        )
    }

    /** Building to construct next, or null. */
    fun chooseBuilding(bot: AiBot, speciesUnits: Array<UnitActor>): Species.Building? {
        if (!bot.difficulty.useBuildings) return null
        val wanted = nextWantedBuilding(bot, speciesUnits.toList()) ?: return null
        if (bot.player.credits < wanted.cost) return null
        return wanted
    }

    /** Credits the bot is saving for its next unlock. 0 when nothing is worth saving for. */
    private fun nextTechReserve(bot: AiBot, units: List<UnitActor>): Int {
        if (!bot.difficulty.useBuildings) return 0
        val wanted = nextWantedBuilding(bot, units) ?: return 0
        if (isDefensiveBuilding(wanted)) return 0
        return wanted.cost
    }

    fun shouldBuildBase(bot: AiBot): Boolean {
        val ctx = bot.context
        if (!bot.difficulty.expandBases) return false
        if (ctx.player.credits < Config.BASE_BUILD_COST) return false
        // Do not expand while there is no army to protect the new base.
        if (ctx.combatUnits.size < 2 && ctx.enemies.isNotEmpty()) return false

        // Bases are production slots and vision, not income. Expand for capacity: enough
        // slots to keep the army coming, plus a little room on large maps. Count bases still
        // under construction so several workers cannot start a batch and overshoot the cap.
        val areaBonus = if (bot.field.tiledWidth * bot.field.tiledHeight >= 900) 1 else 0
        val target = 2 + (bot.difficulty.ambition * 2f).roundToInt() + areaBonus
        return ctx.baseCount < target.coerceAtMost(MAX_BASES)
    }

    private fun chasesUpgrades(bot: AiBot): Boolean =
        phaseOf(bot) != Phase.EARLY && bot.context.income >= 150

    private fun phaseOf(bot: AiBot): Phase {
        val round = bot.battle.turnManager.round
        return when {
            round <= 3 -> Phase.EARLY
            round <= 10 -> Phase.MID
            else -> Phase.LATE
        }
    }

    /**
     * Desired counts per role. Built from production capacity, map size, economy, phase and
     * the enemy army, scaled by difficulty ambition, then capped so a huge map does not ask
     * for an impossible army.
     */
    private fun roleTargets(bot: AiBot, units: List<UnitActor>): Map<UnitRole, Int> {
        val ctx = bot.context
        val production = ctx.baseCount.coerceAtMost(6)
        val scale = bot.difficulty.ambition * mapEconomyScale(bot)
        val roster = units.map { UnitRoles.of(it) }.toSet()
        val phase = phaseOf(bot)
        val targets = HashMap<UnitRole, Int>()

        fun scaled(value: Float): Int = (value * scale).roundToInt()
        fun has(role: UnitRole): Boolean = role in roster

        val enemyArmor = ctx.enemies.count { it.defense >= 25 }
        val enemyArtillery = ctx.enemies.count { UnitRoles.of(it) == UnitRole.ARTILLERY }
        val areaBonus = if (bot.field.tiledWidth * bot.field.tiledHeight >= 900) 1 else 0

        if (has(UnitRole.MELEE)) {
            targets[UnitRole.MELEE] = (scaled(2f + production * 1.5f) + areaBonus)
                .coerceAtLeast(1)
                .coerceAtMost(3 + production)
        }
        if (has(UnitRole.RANGED)) {
            targets[UnitRole.RANGED] = scaled(1f + production * 0.8f)
                .coerceAtMost(1 + production)
        }
        if (has(UnitRole.ARTILLERY) && phase != Phase.EARLY) {
            var target = scaled(0.5f + production * 0.5f)
            if (enemyArmor >= 2) target += 1
            targets[UnitRole.ARTILLERY] = target.coerceAtMost(1 + production / 2)
        }
        if (has(UnitRole.SKIRMISHER) && phase != Phase.EARLY) {
            var target = scaled(0.5f + production * 0.5f)
            if (enemyArtillery > 0) target += 1
            targets[UnitRole.SKIRMISHER] = target.coerceAtMost(1 + production / 2)
        }
        if (has(UnitRole.HEALER) && (production >= 2 || ctx.income >= 200)) {
            targets[UnitRole.HEALER] = scaled(0.5f + production * 0.25f)
                .coerceAtMost(1 + production / 3)
        }
        if (has(UnitRole.AIR) && phase != Phase.EARLY) {
            targets[UnitRole.AIR] = scaled(0.4f + production * 0.3f)
                .coerceAtMost(1 + production / 3)
        }
        if (has(UnitRole.WATER)) {
            targets[UnitRole.WATER] = ctx.ownBases.count { it.isWaterBase }
        }
        if (has(UnitRole.SCOUT) && bot.fogOfWar) {
            targets[UnitRole.SCOUT] = 1
        }
        if (has(UnitRole.WORKER) &&
            (ctx.capturables.isNotEmpty() || bot.capabilities.canBuildBuildings || bot.difficulty.expandBases)
        ) {
            var target = workerTarget(bot)
            if (capturableCount(bot) >= 5) target += 1
            targets[UnitRole.WORKER] = target.coerceAtMost(4)
        }

        return targets
    }

    private fun workerTarget(bot: AiBot): Int =
        ceil(bot.difficulty.ambition * 2.5f).toInt().coerceAtLeast(1)

    private fun capturableCount(bot: AiBot): Int =
        bot.context.capturables.size + bot.field.getCapturablesOf(bot.context.player).size

    /** 0.5..2.0 measure of how rich and large the current map is. */
    private fun mapEconomyScale(bot: AiBot): Float {
        val area = (bot.field.tiledWidth * bot.field.tiledHeight).coerceAtLeast(1)
        val mapScale = (area / MAP_AREA_REFERENCE).coerceIn(0.6f, 2f)
        val eco = ((capturableCount(bot) + bot.context.ownBases.size * 2) / 6f).coerceIn(0.5f, 2f)
        return (mapScale + eco) / 2f
    }

    /**
     * Purchase score. Higher is better value for the role. Price is included with
     * diminishing weight so the bot eventually upgrades instead of only buying the cheapest
     * unit forever.
     */
    private fun scoreUnit(bot: AiBot, unit: UnitActor): Float {
        val attack = unit.cAttack
        val averageDamage = ((attack?.minDamage ?: 0) + (attack?.maxDamage ?: 0)) / 2f
        var power = averageDamage + unit.defense * 1.2f

        val range = attack?.attackRange ?: 1
        if (range > 1) power += averageDamage * 0.06f * (range - 1)
        if ((attack?.attackRangeMin ?: 0) > 0) power -= 10f
        if ((attack?.attackSplashRange ?: 0) > 0) power += (attack?.attackSplashDamage ?: 0) * 0.6f
        if (UnitRoles.isHealerUnit(unit)) power += 30f
        if (unit.canAttackAir) power += 8f
        if (unit.isAir) power += 8f
        if (unit.get(MoveAfterAttackComponent::class.java) != null) power += 10f

        val enemies = bot.context.enemies
        if (enemies.any { it.defense >= 25 } && (attack?.armorPen ?: 0) > 0) power += 15f

        val price = (unit.cShop?.price ?: 1).coerceAtLeast(1)
        return power / sqrt(price.toFloat())
    }

    private fun nextWantedBuilding(bot: AiBot, units: List<UnitActor>): Species.Building? {
        val ctx = bot.context
        val buildings = runCatching { Species.getSpeciesBuildings(ctx.player.species) }.getOrNull()
            ?: return null
        if (buildings.isEmpty()) return null

        val buildable = buildings.filter { building ->
            !ctx.ownsOrBuilding(building.name) &&
                (building.requires.isEmpty() || ctx.ownsBuilding(building.requires))
        }

        // Tech buildings that unlock a unit the army actually wants.
        val desired = units
            .filter { it.cShop != null && !isUnlocked(bot, it) && isUnitWanted(bot, it, units) }
            .flatMap { requiredBuildings(bot, it.selfName) }
            .distinct()
        buildable.firstOrNull { it.name in desired }?.let { return it }

        // Defensive structures: rare, deliberate.
        if (defensiveBuildingCount(bot) < maxDefensiveBuildings(bot) && needsDefense(bot)) {
            buildable
                .filter { isDefensiveBuilding(it) }
                .minByOrNull { it.cost }
                ?.let { return it }
        }

        return null
    }

    private fun maxDefensiveBuildings(bot: AiBot): Int =
        if (bot.difficulty.ambition >= 0.8f) MAX_DEFENSIVE_BUILDINGS + 1 else MAX_DEFENSIVE_BUILDINGS

    private fun isDefensiveBuilding(building: Species.Building): Boolean =
        building.name.contains("wall") || building.name.contains("tower")

    private fun defensiveBuildingCount(bot: AiBot): Int {
        val ctx = bot.context
        val completed = ctx.ownBuildings.count {
            it.selfName.contains("wall") || it.selfName.contains("tower")
        }
        val inProgress = ctx.field.getTiles().count { tile ->
            tile.get(BuildingComponent::class.java)?.let {
                it.playerId == ctx.playerId &&
                    (it.buildingName.contains("wall") || it.buildingName.contains("tower"))
            } == true
        }
        return completed + inProgress
    }

    /**
     * True when an enemy is close to one of our bases AND that base does not already sit
     * next to strong defensive terrain (mountains/forest/tower/fortress...).
     */
    private fun needsDefense(bot: AiBot): Boolean {
        val ctx = bot.context
        if (ctx.enemies.isEmpty() || ctx.ownBases.isEmpty()) return false

        for (base in ctx.ownBases) {
            val threatened = ctx.enemies.any {
                tiledDst(it.tiledX, it.tiledY, base.tiledX, base.tiledY) <= DEFENSE_THREAT_RANGE
            }
            if (!threatened) continue

            val hasStrongTerrain = bot.field.getSurroundingTiles(base.tiledX, base.tiledY).any { tile ->
                val props = TerrainPropsPack.foot[tile.terrain]
                (props.defenseBonus + props.attackBonus) >= STRONG_TERRAIN_BONUS
            }
            if (!hasStrongTerrain) return true
        }
        return false
    }
}
