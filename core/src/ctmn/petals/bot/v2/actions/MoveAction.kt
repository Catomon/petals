package ctmn.petals.bot.v2.actions

import ctmn.petals.bot.v2.AiBot
import ctmn.petals.bot.v2.util.Pathing
import ctmn.petals.bot.v2.util.Targeting
import ctmn.petals.playscreen.commands.MoveUnitCommand
import ctmn.petals.playstage.getLeadUnit
import ctmn.petals.playstage.tiledDst
import ctmn.petals.unit.UnitActor
import ctmn.petals.unit.cLeader
import ctmn.petals.unit.cUnit
import ctmn.petals.unit.canMove
import ctmn.petals.unit.followerID
import ctmn.petals.unit.health
import ctmn.petals.unit.isFollower
import ctmn.petals.unit.isInRange
import ctmn.petals.unit.tiledX
import ctmn.petals.unit.tiledY

/**
 * General movement:
 * - a limited guard detail follows its leader,
 * - scripted levels can roam while no enemy is in aggro range,
 * - everyone else advances towards the current objective.
 */
class MoveAction(bot: AiBot, private val unit: UnitActor) : BotAction(bot) {

    private var command: MoveUnitCommand? = null

    override fun evaluate(): Int {
        if (!unit.canMove()) return IMPOSSIBLE

        // A limited guard detail sticks with the leader; the rest fight independently.
        if (unit.isFollower) {
            val leader = bot.field.getLeadUnit(unit.followerID)
            if (leader != null) {
                val guards = bot.context.followersOf(leader).sortedBy { it.name ?: "" }.take(GUARDS_PER_LEADER)
                if (guards.contains(unit)) {
                    val range = leader.cLeader?.leaderRange ?: 2
                    if (!unit.isInRange(leader.tiledX, leader.tiledY, range)) {
                        return queue(stepTo(leader.tiledX, leader.tiledY, avoidThreat = true), 52)
                    }
                }
            }
        }

        // Aggro / roaming behaviour (mostly for scripted campaign units).
        val settings = bot.settings
        val enemy = bot.context.nearestEnemy(unit)
        val hasAggro = enemy != null &&
            (settings.permaAgro || unit.isInRange(enemy.tiledX, enemy.tiledY, settings.agroRange))
        if (!hasAggro) {
            if (!settings.roamingIfNoAgro) return IMPOSSIBLE
            return roam()
        }

        val objective = Targeting.objective(bot, unit) ?: return IMPOSSIBLE
        // More aggressive difficulties keep pushing at lower health.
        val cautiousAt = (unit.cUnit.baseHealth * (0.45f - 0.25f * bot.difficulty.aggression)).toInt()
        val avoidThreat = unit.health <= cautiousAt
        return queue(stepTo(objective.first, objective.second, avoidThreat = avoidThreat), 40)
    }

    private fun roam(): Int {
        val settings = bot.settings
        val anchor = bot.roamingAnchor(unit)
        val dist = tiledDst(unit.tiledX, unit.tiledY, anchor.first, anchor.second)

        val step = if (dist > settings.roamingMaxRange) {
            Pathing.stepTowards(bot.field, unit, anchor.first, anchor.second, avoidThreat = true)
        } else {
            val tiles = Pathing.reachableTiles(bot.field, unit).filter {
                tiledDst(it.first, it.second, anchor.first, anchor.second) <= settings.roamingMaxRange
            }
            tiles.randomOrNull(bot.random)?.let { bot.field.getTile(it.first, it.second) }
        } ?: return IMPOSSIBLE

        return queue(step, 30)
    }

    private fun stepTo(destX: Int, destY: Int, avoidThreat: Boolean): ctmn.petals.tile.TileActor? =
        Pathing.stepTowards(bot.field, unit, destX, destY, avoidThreat = avoidThreat)

    private fun queue(step: ctmn.petals.tile.TileActor?, priority: Int): Int {
        if (step == null) return IMPOSSIBLE
        val cmd = MoveUnitCommand(unit, step.tiledX, step.tiledY)
        if (!cmd.canExecute(bot.battle)) return IMPOSSIBLE
        command = cmd
        return priority
    }

    override fun execute(): Boolean {
        val cmd = command ?: return false
        return bot.battle.commandManager.queueCommand(cmd, bot.playerID)
    }

    override fun signature(): String = "move:${unit.name}"

    private companion object {
        const val GUARDS_PER_LEADER = 2
    }
}
