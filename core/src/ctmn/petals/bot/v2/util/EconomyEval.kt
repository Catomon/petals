package ctmn.petals.bot.v2.util

import com.badlogic.gdx.utils.Array
import ctmn.petals.Config
import ctmn.petals.bot.v2.AiBot
import ctmn.petals.bot.v2.Difficulty
import ctmn.petals.player.Species
import ctmn.petals.playscreen.selfName
import ctmn.petals.playstage.getTiles
import ctmn.petals.playstage.tiledDst
import ctmn.petals.tile.cPlayerId
import ctmn.petals.tile.components.BuildingComponent
import ctmn.petals.tile.isWaterBase
import ctmn.petals.unit.Ability
import ctmn.petals.unit.TerrainPropsPack
import ctmn.petals.unit.UnitActor
import ctmn.petals.unit.UnitIds
import ctmn.petals.unit.cAbilities
import ctmn.petals.unit.cAttack
import ctmn.petals.unit.cShop
import ctmn.petals.unit.canAttackAir
import ctmn.petals.unit.isAir
import ctmn.petals.unit.isWater
import ctmn.petals.unit.isWorker
import ctmn.petals.unit.movingRange
import ctmn.petals.unit.tiledX
import ctmn.petals.unit.tiledY
import ctmn.petals.utils.getSurroundingTiles

/** Economy decisions: what to buy, what to build and when to expand. */
object EconomyEval {

    private val healingAbilityNames =
        setOf("healing", "healing_touch", "health_potion", "personal_barrier")

    /** Cheap, fast, weak flyers meant for vision, not for fighting. */
    private val scoutUnitIds = setOf(UnitIds.PIXIE, UnitIds.GOBLIN_SCOUT)

    private val siegeUnitIds = setOf(UnitIds.CATAPULT, UnitIds.GOBLIN_CATAPULT, UnitIds.DOLL_PEAS)

    private const val MAX_DEFENSIVE_BUILDINGS = 2

    /** How close an enemy has to be to a base before the bot considers walls. */
    private const val DEFENSE_THREAT_RANGE = 6

    /** Terrain is "strong" if it gives at least this much combat bonus. */
    private const val STRONG_TERRAIN_BONUS = 8

    private enum class UnitRole { WORKER, SCOUT, HEALER, SIEGE, RANGED, AIR, WATER, MELEE }

    private class ArmyTargets(
        val melee: Int,
        val ranged: Int,
        val healer: Int,
        val siege: Int,
        val air: Int,
    )

    fun isHealerUnit(unit: UnitActor): Boolean {
        val abilities = unit.cAbilities?.abilities ?: return false
        return abilities.any { it.type == Ability.Type.HEALING || it.name in healingAbilityNames }
    }

    fun isScoutUnit(unit: UnitActor): Boolean {
        if (unit.selfName in scoutUnitIds) return true
        val price = unit.cShop?.price ?: return false
        return price <= 60 && unit.movingRange >= 3 && (unit.cAttack?.attackRange ?: 1) <= 1
    }

    private fun roleOf(unit: UnitActor): UnitRole {
        if (unit.isWorker) return UnitRole.WORKER
        if (isScoutUnit(unit)) return UnitRole.SCOUT
        if (isHealerUnit(unit)) return UnitRole.HEALER
        if (unit.selfName in siegeUnitIds) return UnitRole.SIEGE
        if (unit.isWater) return UnitRole.WATER
        if (unit.isAir) return UnitRole.AIR

        return when {
            (unit.cAttack?.attackRange ?: 0) >= 3 -> UnitRole.SIEGE
            (unit.cAttack?.attackRange ?: 0) >= 2 -> UnitRole.RANGED
            else -> UnitRole.MELEE
        }
    }

    private fun targetsFor(difficulty: Difficulty): ArmyTargets = when (difficulty) {
        Difficulty.EASY -> ArmyTargets(melee = 2, ranged = 1, healer = 0, siege = 0, air = 0)
        Difficulty.NORMAL -> ArmyTargets(melee = 4, ranged = 2, healer = 1, siege = 1, air = 1)
        Difficulty.HARD -> ArmyTargets(melee = 5, ranged = 3, healer = 1, siege = 2, air = 2)
    }

    fun requiredBuildings(bot: AiBot, unitName: String): List<String> =
        runCatching {
            Species.getSpeciesUnits(bot.player.species)
                .firstOrNull { it.unitActor.selfName == unitName }
                ?.requiredBuildings ?: emptyList()
        }.getOrDefault(emptyList())

    fun isUnlocked(bot: AiBot, unit: UnitActor): Boolean =
        requiredBuildings(bot, unit.selfName).all { bot.context.ownsBuilding(it) }

    /** True if this unit still fills a gap in the current army composition. */
    fun isUnitWanted(bot: AiBot, unit: UnitActor): Boolean {
        val role = roleOf(unit)
        if (role == UnitRole.WORKER) return bot.context.workers.size < bot.difficulty.maxWorkers
        if (role == UnitRole.SCOUT) return bot.playScreen.fogOfWarManager.drawFog

        val targets = targetsFor(bot.difficulty)
        val count = bot.context.units.count { roleOf(it) == role }
        return when (role) {
            UnitRole.HEALER -> count < targets.healer
            UnitRole.RANGED -> count < targets.ranged
            UnitRole.SIEGE -> count < targets.siege
            UnitRole.MELEE -> count < targets.melee
            UnitRole.AIR -> count < targets.air
            UnitRole.WATER -> count < targets.melee
            else -> false
        }
    }

    /**
     * Name of a unit to buy at a free base, or null if nothing is worth buying.
     * The bot keeps a role based army composition instead of spamming the cheapest unit.
     */
    fun chooseUnitToBuy(bot: AiBot, speciesUnits: Array<UnitActor>): String? {
        val ctx = bot.context
        val player = ctx.player
        if (player.credits <= 0) return null

        val base = ctx.freeOwnedBases().firstOrNull() ?: return null
        val isWater = base.isWaterBase

        val candidates = speciesUnits.filter { unit ->
            val shop = unit.cShop ?: return@filter false
            player.credits >= shop.price &&
                isUnlocked(bot, unit) &&
                if (isWater) unit.isWater else !unit.isWater
        }.toList()
        if (candidates.isEmpty()) return null

        fun cheapest(list: List<UnitActor>) = list.minByOrNull { it.cShop!!.price }
        fun countRole(role: UnitRole) = ctx.units.count { roleOf(it) == role }

        // Keep enough workers to actually run the economy.
        val wantsWorker = ctx.workers.size < bot.difficulty.maxWorkers &&
            (ctx.capturables.isNotEmpty() || bot.capabilities.canBuildBuildings || bot.difficulty.expandBases)
        if (wantsWorker) cheapest(candidates.filter { it.isWorker })?.let { return it.selfName }

        val combat = candidates.filter { !it.isWorker }
        if (combat.isEmpty()) return null

        // Emergency air defense (never a scout).
        val enemyAir = ctx.enemies.count { it.isAir }
        val ownAirDefense = ctx.units.count { it.canAttackAir }
        if (enemyAir > ownAirDefense) {
            cheapest(combat.filter { it.canAttackAir && roleOf(it) != UnitRole.SCOUT })
                ?.let { return it.selfName }
        }

        val targets = targetsFor(bot.difficulty)

        fun buyIfBelow(role: UnitRole, target: Int): String? {
            if (target <= 0 || countRole(role) >= target) return null
            return cheapest(combat.filter { roleOf(it) == role })?.selfName
        }

        buyIfBelow(UnitRole.HEALER, targets.healer)?.let { return it }
        buyIfBelow(UnitRole.RANGED, targets.ranged)?.let { return it }
        buyIfBelow(UnitRole.SIEGE, targets.siege)?.let { return it }
        buyIfBelow(UnitRole.MELEE, targets.melee)?.let { return it }
        buyIfBelow(UnitRole.AIR, targets.air)?.let { return it }
        if (isWater) buyIfBelow(UnitRole.WATER, targets.melee)?.let { return it }

        // A single scout is only worth it when the map actually has fog of war.
        if (bot.playScreen.fogOfWarManager.drawFog && countRole(UnitRole.SCOUT) < 1) {
            cheapest(combat.filter { roleOf(it) == UnitRole.SCOUT })?.let { return it.selfName }
        }

        // Everything is at its target: bank the credits for tech/units instead of spamming.
        return null
    }

    /**
     * Building to construct next, or null.
     *
     * The bot will:
     * - build a tech building only when it unlocks a unit the army actually wants,
     * - never build a second copy of a building that is finished or under construction,
     * - only build walls/towers when a base is threatened and lacks strong terrain nearby,
     *   and never more than [MAX_DEFENSIVE_BUILDINGS] of them.
     */
    fun chooseBuilding(bot: AiBot, speciesUnits: Array<UnitActor>): Species.Building? {
        val ctx = bot.context
        val player = ctx.player
        val buildings = runCatching { Species.getSpeciesBuildings(player.species) }.getOrNull() ?: return null
        if (buildings.size == 0) return null

        val buildable = buildings.filter { building ->
            player.credits >= building.cost &&
                !ctx.ownsOrBuilding(building.name) &&
                (building.requires.isEmpty() || ctx.ownsBuilding(building.requires))
        }
        if (buildable.isEmpty()) return null

        // Tech buildings that unlock a unit we actually want.
        val desired = speciesUnits
            .filter { it.cShop != null && !isUnlocked(bot, it) && isUnitWanted(bot, it) }
            .flatMap { requiredBuildings(bot, it.selfName) }
            .distinct()

        buildable.firstOrNull { it.name in desired }?.let { return it }

        // Defensive structures: rare, deliberate.
        if (defensiveBuildingCount(bot) < MAX_DEFENSIVE_BUILDINGS && needsDefense(bot)) {
            buildable
                .filter { it.name.contains("wall") || it.name.contains("tower") }
                .minByOrNull { it.cost }
                ?.let { return it }
        }

        return null
    }

    private fun defensiveBuildingCount(bot: AiBot): Int {
        val ctx = bot.context
        val completed = ctx.ownBuildings.count {
            it.selfName.contains("wall") || it.selfName.contains("tower")
        }
        val inProgress = ctx.playStage.getTiles().count { tile ->
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

            val hasStrongTerrain = bot.playStage.getSurroundingTiles(base.tiledX, base.tiledY).any { tile ->
                val props = TerrainPropsPack.foot[tile.terrain]
                (props.defenseBonus + props.attackBonus) >= STRONG_TERRAIN_BONUS
            }
            if (!hasStrongTerrain) return true
        }
        return false
    }

    fun shouldBuildBase(bot: AiBot): Boolean {
        val ctx = bot.context
        if (!bot.difficulty.expandBases) return false
        if (ctx.player.credits < Config.BASE_BUILD_COST) return false

        val incomePerBase = 300
        val desiredBases = 1 + ctx.income / incomePerBase
        return ctx.ownBases.size < desiredBases
    }
}
