package ctmn.petals.bot.v2.actions

import ctmn.petals.bot.v2.AiBot
import ctmn.petals.bot.v2.util.Pathing
import ctmn.petals.bot.v2.util.Targeting
import ctmn.petals.playscreen.commands.UseAbilityCommand
import ctmn.petals.playscreen.selfName
import ctmn.petals.playstage.getUnitsForLeader
import ctmn.petals.playstage.tiledDst
import ctmn.petals.tile.TileActor
import ctmn.petals.unit.Ability
import ctmn.petals.unit.UnitActor
import ctmn.petals.unit.Units
import ctmn.petals.unit.actionPoints
import ctmn.petals.unit.abilities.SummonAbility
import ctmn.petals.unit.cAbilities
import ctmn.petals.unit.cSummonable
import ctmn.petals.unit.cSummoner
import ctmn.petals.unit.cUnit
import ctmn.petals.unit.health
import ctmn.petals.unit.isAlly
import ctmn.petals.unit.isInRange
import ctmn.petals.unit.isLeader
import ctmn.petals.unit.leaderID
import ctmn.petals.unit.mana
import ctmn.petals.unit.tiledX
import ctmn.petals.unit.tiledY

/**
 * Generic ability usage. Classifies each ability (support / offense / movement / summon),
 * finds the best cast tile and lets the command validate the rest.
 */
class AbilityAction(bot: AiBot, private val unit: UnitActor) : BotAction(bot) {

    private var command: UseAbilityCommand? = null

    private enum class Category { SUPPORT, OFFENSE, MOVEMENT, SUMMON, OTHER }

    private class Scored(val command: UseAbilityCommand, val priority: Int)

    private val healAbilityNames = setOf("healing", "healing_touch", "health_potion")

    override fun evaluate(): Int {
        if (!bot.difficulty.useAbilities) return IMPOSSIBLE
        val abilities = unit.cAbilities?.abilities ?: return IMPOSSIBLE
        if (abilities.size == 0) return IMPOSSIBLE

        var best: Scored? = null
        for (ability in abilities.toList()) {
            val scored = evaluateAbility(ability) ?: continue
            if (best == null || scored.priority > best.priority) best = scored
        }

        val result = best ?: return IMPOSSIBLE
        command = result.command
        return result.priority
    }

    private fun evaluateAbility(ability: Ability): Scored? {
        val cAbilities = unit.cAbilities ?: return null
        if (!cAbilities.abilities.contains(ability)) return null
        if (ability.currentCooldown > 0) return null
        if (unit.mana < ability.cost) return null
        if (unit.actionPoints <= 0 && ability.castAmountsLeft == ability.castAmounts) return null

        if (ability is SummonAbility) return evaluateSummon(ability)

        val category = category(ability)
        if (category == Category.OTHER) return null

        var best: Scored? = null
        for (tile in candidateTiles(ability, category)) {
            val cmd = UseAbilityCommand(unit, ability, tile.tiledX, tile.tiledY)
            if (!runCatching { cmd.canExecute(bot.playScreen) }.getOrDefault(false)) continue

            val score = scoreCast(ability, category, tile.tiledX, tile.tiledY)
            val priority = priorityFor(category, score)
            if (priority == IMPOSSIBLE) continue
            if (best == null || priority > best.priority) best = Scored(cmd, priority)
        }
        return best
    }

    private fun category(ability: Ability): Category = when {
        ability is SummonAbility -> Category.SUMMON
        ability.type == Ability.Type.HEALING -> Category.SUPPORT
        ability.type == Ability.Type.BUFF -> Category.SUPPORT
        ability.type == Ability.Type.DAMAGE -> Category.OFFENSE
        ability.type == Ability.Type.CONTROL -> Category.OFFENSE
        ability.type == Ability.Type.DEBUFF -> Category.OFFENSE
        ability.type == Ability.Type.MOVEMENT -> Category.MOVEMENT
        ability.target == Ability.Target.ENEMY_UNIT -> Category.OFFENSE
        ability.target == Ability.Target.MY_UNIT -> Category.SUPPORT
        ability.target == Ability.Target.MY_TEAM_UNIT -> Category.SUPPORT
        ability.target == Ability.Target.ALLY_UNIT -> Category.SUPPORT
        ability.target == Ability.Target.TILE_PASSABLE -> Category.MOVEMENT
        ability.target == Ability.Target.TILE_PASSABLE_INCLUDE_SELF -> Category.MOVEMENT
        else -> Category.OTHER
    }

    private fun candidateTiles(ability: Ability, category: Category): List<TileActor> {
        val range = ability.range.coerceAtLeast(0)
        val stage = bot.playStage
        val tiles = when (category) {
            Category.SUPPORT -> bot.context.units
                .filter { it.isInRange(unit.tiledX, unit.tiledY, range) }
                .mapNotNull { stage.getTile(it.tiledX, it.tiledY) }

            Category.OFFENSE -> bot.context.enemies
                .filter { it.isInRange(unit.tiledX, unit.tiledY, range) }
                .mapNotNull { stage.getTile(it.tiledX, it.tiledY) }

            Category.MOVEMENT -> Pathing.freeTilesInRange(stage, unit, range)

            else -> emptyList()
        }
        return tiles.distinctBy { it.tiledX to it.tiledY }
    }

    private fun scoreCast(ability: Ability, category: Category, x: Int, y: Int): Int {
        val targets = ability.getTargets(bot.playStage, unit, x, y).filterIsInstance<UnitActor>()
        return when (category) {
            Category.SUPPORT -> {
                val allies = targets.filter { it.isAlly(unit) }
                if (allies.isEmpty()) return 0
                val missingSum = allies.sumOf { (it.cUnit.baseHealth - it.health).coerceAtLeast(0) }
                val isHeal = ability.type == Ability.Type.HEALING || ability.name in healAbilityNames
                if (isHeal) missingSum else allies.size * 5
            }

            Category.OFFENSE -> {
                var score = 0
                for (target in targets) {
                    if (target.isAlly(unit)) continue
                    score += 20
                    val missing = (target.cUnit.baseHealth - target.health).coerceAtLeast(0)
                    score += missing / 2
                    if (target.health <= 30) score += 30
                    if (target.isLeader) score += 15
                }
                score
            }

            Category.MOVEMENT -> {
                val objective = Targeting.objective(bot, unit) ?: return 5
                val before = tiledDst(unit.tiledX, unit.tiledY, objective.first, objective.second)
                val after = tiledDst(x, y, objective.first, objective.second)
                (before - after) * 5
            }

            else -> 0
        }
    }

    private fun priorityFor(category: Category, score: Int): Int = when (category) {
        Category.SUPPORT -> if (score >= 40) 86 else if (score > 0) 55 else IMPOSSIBLE
        Category.OFFENSE -> if (score >= 50) 88 else if (score > 0) 64 else IMPOSSIBLE
        Category.MOVEMENT -> if (score > 0) 45 else IMPOSSIBLE
        else -> IMPOSSIBLE
    }

    private fun evaluateSummon(ability: SummonAbility): Scored? {
        val summoner = unit.cSummoner ?: return null
        val selected = summoner.selectedUnit
            ?: summoner.units.maxByOrNull { Units.get(it).cSummonable?.cost ?: 0 }
            ?: return null
        summoner.selectedUnit = selected

        val owned = bot.playStage.getUnitsForLeader(unit.leaderID, true)
            .count { it.selfName in summoner.units }
        if (owned >= summoner.maxUnits) return null

        val tiles = Pathing.freeTilesInRange(bot.playStage, unit, ability.range)
        if (tiles.isEmpty()) return null

        val enemy = bot.context.nearestEnemy(unit)
        var best: Scored? = null
        for (tile in tiles) {
            val cmd = UseAbilityCommand(unit, ability, tile.tiledX, tile.tiledY)
            if (!runCatching { cmd.canExecute(bot.playScreen) }.getOrDefault(false)) continue

            val score = if (enemy == null) 30
            else 70 - tiledDst(tile.tiledX, tile.tiledY, enemy.tiledX, enemy.tiledY)
            if (best == null || score > best.priority) best = Scored(cmd, score)
        }
        return best
    }

    override fun execute(): Boolean {
        val cmd = command ?: return false
        return bot.playScreen.commandManager.queueCommand(cmd, bot.playerID)
    }

    override fun signature(): String = "ability:${unit.name}"
}
