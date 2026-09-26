package ctmn.petals.bot.v2.util

import ctmn.petals.bot.v2.AiBot
import ctmn.petals.playstage.tiledDst
import ctmn.petals.unit.UnitActor
import ctmn.petals.unit.tiledX
import ctmn.petals.unit.tiledY

/** Where a unit wants to go when it has no immediate tactical target. */
object Targeting {

    fun objective(bot: AiBot, unit: UnitActor): Pair<Int, Int>? {
        val ctx = bot.context

        ctx.nearestEnemy(unit)?.let { return it.tiledX to it.tiledY }
        // Roaming creature bots never push to bases: they only react to units.
        if (bot.settings.attackBuildings) {
            ctx.nearestEnemyBase(unit)?.let { return it.tiledX to it.tiledY }
        }
        ctx.capturables.minByOrNull { tiledDst(unit.tiledX, unit.tiledY, it.tiledX, it.tiledY) }
            ?.let { return it.tiledX to it.tiledY }

        return null
    }
}
