package ctmn.petals.bot.v2.util

import ctmn.petals.playscreen.PlayScreen
import ctmn.petals.playscreen.playStageOrNull
import ctmn.petals.playstage.getUnits
import ctmn.petals.playstage.tiledDst
import ctmn.petals.unit.UnitActor
import ctmn.petals.unit.cAttack
import ctmn.petals.unit.cUnit
import ctmn.petals.unit.calculateDmgDef
import ctmn.petals.unit.canAttackNow
import ctmn.petals.unit.health
import ctmn.petals.unit.isAlly
import ctmn.petals.unit.isLeader
import ctmn.petals.unit.maxDamage
import ctmn.petals.unit.minDamage
import ctmn.petals.unit.tiledX
import ctmn.petals.unit.tiledY

/** Pure combat math helpers so actions share the same evaluation. */
object CombatEval {

    fun expectedDamage(playScreen: PlayScreen, attacker: UnitActor, defender: UnitActor): Int =
        runCatching { playScreen.calculateDmgDef(attacker, defender).first }
            .getOrDefault(attacker.minDamage)

    fun canHit(attacker: UnitActor, defender: UnitActor): Boolean =
        runCatching { attacker.canAttackNow(defender) }.getOrDefault(false)

    fun killsInOneHit(playScreen: PlayScreen, attacker: UnitActor, defender: UnitActor): Boolean =
        expectedDamage(playScreen, attacker, defender) >= defender.health

    /**
     * Higher is a better target. Rewards kills, focus fire on wounded units and taking out
     * leaders. [skill] (0..1) controls how much the bot cares about good trades.
     */
    fun attackScore(playScreen: PlayScreen, attacker: UnitActor, defender: UnitActor, skill: Float): Int {
        if (!canHit(attacker, defender)) return 0

        val damage = expectedDamage(playScreen, attacker, defender)
        val missingHealth = (defender.cUnit.baseHealth - defender.health).coerceAtLeast(0)

        var score = damage
        score += (missingHealth * 0.5f).toInt()
        if (defender.health <= damage) score += 250
        if (defender.isLeader) score += (60 * skill).toInt()

        return score
    }

    /** Rough estimate of how dangerous a tile is to stand on: reachable enemy damage. */
    fun threatAt(unit: UnitActor, tileX: Int, tileY: Int): Int {
        val stage = unit.playStageOrNull ?: return 0
        var threat = 0
        for (enemy in stage.getUnits()) {
            if (enemy.isAlly(unit)) continue
            val range = enemy.cAttack?.attackRange ?: continue
            if (tiledDst(enemy.tiledX, enemy.tiledY, tileX, tileY) <= range) {
                threat += enemy.maxDamage
            }
        }
        return threat
    }
}
