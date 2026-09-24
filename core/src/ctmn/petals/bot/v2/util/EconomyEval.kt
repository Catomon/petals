package ctmn.petals.bot.v2.util

import com.badlogic.gdx.utils.Array
import ctmn.petals.Config
import ctmn.petals.bot.v2.AiBot
import ctmn.petals.player.Species
import ctmn.petals.playscreen.selfName
import ctmn.petals.tile.isWaterBase
import ctmn.petals.unit.Ability
import ctmn.petals.unit.UnitActor
import ctmn.petals.unit.cAbilities
import ctmn.petals.unit.cAttack
import ctmn.petals.unit.cShop
import ctmn.petals.unit.canAttackAir
import ctmn.petals.unit.isAir
import ctmn.petals.unit.isWater
import ctmn.petals.unit.isWorker

/** Economy decisions: what to buy, what to build and when to expand. */
object EconomyEval {

    private val healingAbilityNames =
        setOf("healing", "healing_touch", "health_potion", "personal_barrier")

    fun isHealerUnit(unit: UnitActor): Boolean {
        val abilities = unit.cAbilities?.abilities ?: return false
        return abilities.any { it.type == Ability.Type.HEALING || it.name in healingAbilityNames }
    }

    fun requiredBuildings(bot: AiBot, unitName: String): List<String> =
        runCatching {
            Species.getSpeciesUnits(bot.player.species)
                .firstOrNull { it.unitActor.selfName == unitName }
                ?.requiredBuildings ?: emptyList()
        }.getOrDefault(emptyList())

    fun isUnlocked(bot: AiBot, unit: UnitActor): Boolean =
        requiredBuildings(bot, unit.selfName).all { bot.context.ownsBuilding(it) }

    /** Name of a unit to buy at a free base, or null if nothing is worth buying. */
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

        // keep enough workers to run the economy
        if (ctx.workers.size < bot.difficulty.maxWorkers && ctx.capturables.isNotEmpty()) {
            cheapest(candidates.filter { it.isWorker })?.let { return it.selfName }
        }

        // workers are not combat units, only buy them for the economy
        val combat = candidates.filter { !it.isWorker }
        if (combat.isEmpty()) return null

        // answer enemy air
        val enemyAir = ctx.enemies.count { it.isAir }
        val ownAirDefense = ctx.units.count { it.canAttackAir }
        if (enemyAir > ownAirDefense) {
            cheapest(combat.filter { it.canAttackAir })?.let { return it.selfName }
        }

        // keep a balanced army
        if (ctx.units.none { isHealerUnit(it) }) {
            cheapest(combat.filter { isHealerUnit(it) })?.let { return it.selfName }
        }

        val rangedCount = ctx.units.count { (it.cAttack?.attackRange ?: 0) >= 2 }
        if (rangedCount < 2) {
            cheapest(combat.filter { (it.cAttack?.attackRange ?: 0) >= 2 })?.let { return it.selfName }
        }

        cheapest(combat.filter { (it.cAttack?.attackRange ?: 0) <= 1 })?.let { return it.selfName }

        return cheapest(combat)?.selfName
    }

    /** Building to construct next, or null. */
    fun chooseBuilding(bot: AiBot, speciesUnits: Array<UnitActor>): Species.Building? {
        val ctx = bot.context
        val player = ctx.player
        val buildings = runCatching { Species.getSpeciesBuildings(player.species) }.getOrNull() ?: return null

        val desired = speciesUnits
            .filter { it.cShop != null && !isUnlocked(bot, it) }
            .flatMap { requiredBuildings(bot, it.selfName) }
            .distinct()
            .filter { !ctx.ownsBuilding(it) }

        val affordable = buildings.filter { building ->
            player.credits >= building.cost &&
                !ctx.ownsBuilding(building.name) &&
                (building.requires.isEmpty() || ctx.ownsBuilding(building.requires))
        }

        affordable.firstOrNull { it.name in desired }?.let { return it }

        // defensive fallback
        return affordable.firstOrNull { it.name.contains("wall") || it.name.contains("tower") }
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
