package ctmn.petals.sim

import ctmn.petals.Config
import ctmn.petals.playstage.damageTile
import ctmn.petals.unit.UnitActor
import ctmn.petals.unit.actionPoints
import ctmn.petals.unit.attackRange
import ctmn.petals.unit.cAttack
import ctmn.petals.unit.cUnit
import ctmn.petals.unit.calculateDmgDef
import ctmn.petals.unit.canAttack
import ctmn.petals.unit.combatDamageHpMod
import ctmn.petals.unit.dealDamage
import ctmn.petals.unit.health
import ctmn.petals.unit.isAir
import ctmn.petals.unit.isAlly
import ctmn.petals.unit.isUnitNear
import ctmn.petals.unit.killedBy
import ctmn.petals.unit.tiledX
import ctmn.petals.unit.tiledY
import ctmn.petals.unit.component.ATTACK_TYPE_GROUND
import ctmn.petals.unit.component.InvisibilityComponent
import ctmn.petals.unit.component.MoveAfterAttackComponent
import ctmn.petals.unit.component.ReloadingComponent
import ctmn.petals.utils.getUnitsInRange
import kotlin.math.max

/** Everything the view needs to animate an attack whose state was already resolved. */
class AttackOutcome(
    val miss: Boolean,
    val crit: Boolean,
    val splashHit: Boolean,
    val retaliated: Boolean,
)

/**
 * Resolves a full attack immediately: action points, damage, deaths, splash, retaliation and
 * the post attack effects. All randomness is consumed here, in a fixed order, so client and
 * server stay deterministic and the headless server never depends on animations.
 */
fun resolveAttack(battle: Battle, attackerUnit: UnitActor, targetUnit: UnitActor): AttackOutcome {
    attackerUnit.actionPoints -= Config.ACTION_POINTS_ATTACK

    attackerUnit.del(InvisibilityComponent::class.java)
    targetUnit.del(InvisibilityComponent::class.java)

    val attackerPair = battle.calculateDmgDef(attackerUnit, targetUnit)
    val defenderPair = battle.calculateDmgDef(targetUnit, attackerUnit)

    var targetDamage = attackerPair.first - defenderPair.second
    if (targetDamage < 1) targetDamage = 0

    var attackerDamage = defenderPair.first - attackerPair.second
    if (attackerDamage < 1) attackerDamage = 0

    val miss = if (Config.MISSES) battle.random.nextInt(1, 101) <= 5 else false
    val crit = if (Config.CRITS) battle.random.nextInt(1, 101) <= 5 else false

    var splashHit = false

    if (!miss) {
        val finalTargetDamage = if (crit) (targetDamage * 1.5f).toInt() else targetDamage

        targetUnit.dealDamage(finalTargetDamage, attackerUnit, battle, false)
        if (targetUnit.health <= 0) targetUnit.killedBy(attackerUnit, battle)

        // Splash damage
        val attackerAttack = attackerUnit.cAttack
        if (attackerAttack != null && attackerAttack.attackSplashRange > 0) {
            val unitsInRange = battle.field.getUnitsInRange(
                targetUnit.tiledX,
                targetUnit.tiledY,
                attackerAttack.attackSplashRange
            )
            unitsInRange.forEach { other ->
                if ((other.tiledX != targetUnit.tiledX || other.tiledY != targetUnit.tiledY) &&
                    !other.isAlly(attackerUnit) && attackerUnit.canAttack(other)
                ) {
                    splashHit = true
                    other.dealDamage(
                        (max(
                            battle.random.nextInt(
                                attackerAttack.attackSplashDamage - 5,
                                attackerAttack.attackSplashDamage
                            ), 1
                        ) * attackerUnit.combatDamageHpMod).toInt(),
                        attackerUnit,
                        battle,
                        true
                    )
                }
            }
        }
    }

    // Retaliation
    var retaliated = false
    val attackTypeNotOk = targetUnit.isAir && targetUnit.cAttack?.attackType == ATTACK_TYPE_GROUND
    if (Config.ATTACK_BACK
        && targetUnit.attackRange > 0
        && !attackTypeNotOk
        && targetUnit.canAttack(attackerUnit)
        && attackerUnit.isUnitNear(targetUnit, 1)
        && targetUnit.cAttack!!.attackRangeMin <= 0
        && ((!attackerUnit.isAir || (attackerUnit.isAir && targetUnit.isAir)) || targetUnit.attackRange > 1)
    ) {
        retaliated = true
        attackerUnit.dealDamage(attackerDamage, targetUnit, battle, false)
        if (attackerUnit.health <= 0) attackerUnit.killedBy(targetUnit, battle)

        targetUnit.get(ReloadingComponent::class.java)?.apply {
            currentTurns = turns
        }
    }

    // Post attack effects, applied even on a miss, matching the old animation callbacks.
    attackerUnit.get(ReloadingComponent::class.java)?.apply {
        currentTurns = turns
    }

    attackerUnit.get(MoveAfterAttackComponent::class.java)?.apply {
        if (!attacked) {
            attackerUnit.actionPoints = Config.ACTION_POINTS
            attackerUnit.cUnit.movingRange = secondRange
            attacked = true
        }
    }

    battle.field.getTile(targetUnit.tiledX, targetUnit.tiledY)?.let { tile ->
        battle.field.damageTile(tile, attackerUnit.cAttack?.environmentDmg ?: 0)
    }

    return AttackOutcome(miss = miss, crit = crit, splashHit = splashHit, retaliated = retaliated)
}
