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
import ctmn.petals.utils.GameLog
import ctmn.petals.utils.getUnitsInRange
import ctmn.petals.playscreen.selfName
import ctmn.petals.playscreen.stageName
import kotlin.math.max

/** Everything the view needs to animate an attack whose state was already resolved. */
class AttackOutcome(
    val miss: Boolean,
    val crit: Boolean,
    val splashHit: Boolean,
    val retaliated: Boolean,
)

/** One unit caught by splash, and what it takes. */
class SplashHit(
    var unitId: String = "",
    var damage: Int = 0,
)

/**
 * What one attack came to.
 *
 * The server decides this, once, and it travels with the command, so every client applies the
 * same damage, the same deaths and the same effects. A client that rolled its own numbers applied
 * its own outcome, which is how a unit came to die on one screen and live on another.
 *
 * The damage here is already resolved: terrain, matchup, buffs, crit and splash are all in the
 * numbers. The client still runs [applyAttack] with it, so shields, health and death stay in one
 * place rather than being worked out twice.
 */
class AttackDecision {
    var attackerUnitId: String = ""
    var targetUnitId: String = ""

    /** What the target takes, and what the attacker takes back. */
    var targetDamage: Int = 0
    var attackerDamage: Int = 0

    var miss: Boolean = false
    var crit: Boolean = false
    var retaliated: Boolean = false

    /** Other units the attack catches, by unit id. A client skips any it cannot see. */
    var splash: MutableList<SplashHit> = mutableListOf()

    /** Terrain damage the attack does to the tile the target stands on. */
    var tileDamage: Int = 0
}

/**
 * Rolls an attack without touching anything: the damage each side takes, misses, crits, and who
 * is caught by splash. The server runs this, and a client applies what it decided instead of
 * rolling its own.
 *
 * The rolls happen in a fixed order, so a single player game and a server game roll the same way.
 */
fun decideAttack(battle: Battle, attackerUnit: UnitActor, targetUnit: UnitActor): AttackDecision {
    val decision = AttackDecision()
    decision.attackerUnitId = attackerUnit.stageName
    decision.targetUnitId = targetUnit.stageName

    val attackerPair = battle.calculateDmgDef(attackerUnit, targetUnit)
    val defenderPair = battle.calculateDmgDef(targetUnit, attackerUnit)

    var targetDamage = attackerPair.first - defenderPair.second
    if (targetDamage < 1) targetDamage = 0

    var attackerDamage = defenderPair.first - attackerPair.second
    if (attackerDamage < 1) attackerDamage = 0

    decision.miss = if (Config.MISSES) battle.random.nextInt(1, 101) <= 5 else false
    decision.crit = if (Config.CRITS) battle.random.nextInt(1, 101) <= 5 else false

    if (!decision.miss) {
        if (decision.crit) targetDamage = (targetDamage * 1.5f).toInt()
        decision.targetDamage = targetDamage

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
                    decision.splash.add(
                        SplashHit(
                            other.stageName,
                            (max(
                                battle.random.nextInt(
                                    attackerAttack.attackSplashDamage - 5,
                                    attackerAttack.attackSplashDamage
                                ), 1
                            ) * attackerUnit.combatDamageHpMod).toInt(),
                        )
                    )
                }
            }
        }
    }

    // Retaliation. Whether the target hits back reads the state, so it belongs to the decision
    // and not to the application.
    val attackTypeNotOk = targetUnit.isAir && targetUnit.cAttack?.attackType == ATTACK_TYPE_GROUND
    decision.retaliated = Config.ATTACK_BACK &&
            targetUnit.attackRange > 0 &&
            !attackTypeNotOk &&
            targetUnit.canAttack(attackerUnit) &&
            attackerUnit.isUnitNear(targetUnit, 1) &&
            targetUnit.cAttack!!.attackRangeMin <= 0 &&
            ((!attackerUnit.isAir || (attackerUnit.isAir && targetUnit.isAir)) || targetUnit.attackRange > 1)

    if (decision.retaliated) decision.attackerDamage = attackerDamage

    decision.tileDamage = attackerUnit.cAttack?.environmentDmg ?: 0

    // One line per attack, on both sides, so two logs can be put next to each other and the
    // numbers compared. That is how a divergence shows up before it becomes a unit that died on
    // one screen and not the other.
    GameLog.info(
        "CombatRules",
        "${attackerUnit.selfName} -> ${targetUnit.selfName}: $targetDamage damage" +
                (if (decision.miss) " (miss)" else "") +
                (if (decision.crit) " (crit)" else "") +
                ", retaliation ${decision.attackerDamage}"
    )

    return decision
}

/**
 * Applies a decided attack: action points, damage, deaths, splash, retaliation, the post attack
 * effects, and the request for the animation. Runs on every machine, from the same decision.
 */
fun applyAttack(
    battle: Battle,
    attackerUnit: UnitActor,
    targetUnit: UnitActor,
    decision: AttackDecision,
): AttackOutcome {
    attackerUnit.actionPoints -= Config.ACTION_POINTS_ATTACK

    attackerUnit.del(InvisibilityComponent::class.java)
    targetUnit.del(InvisibilityComponent::class.java)

    if (!decision.miss) {
        targetUnit.dealDamage(decision.targetDamage, attackerUnit, battle, false)
        if (targetUnit.health <= 0) targetUnit.killedBy(attackerUnit, battle)

        decision.splash.forEach { hit ->
            val other = battle.field.findUnit(hit.unitId) ?: return@forEach
            other.dealDamage(hit.damage, attackerUnit, battle, true)
        }
    }

    // Retaliation
    if (decision.retaliated) {
        attackerUnit.dealDamage(decision.attackerDamage, targetUnit, battle, false)
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
        battle.field.damageTile(tile, decision.tileDamage)
    }

    // What was actually applied, on every machine. The server decides and the clients apply, so
    // this is the line to put next to the server's when checking that both sides landed the same
    // damage. Quiet by default, since the server's decision is already logged.
    GameLog.verbose(
        "CombatRules",
        "applied: ${attackerUnit.selfName} -> ${targetUnit.selfName} ${decision.targetDamage} damage" +
                (if (decision.splash.isEmpty()) "" else
                    ", splash " + decision.splash.joinToString(" ") { "${it.unitId}=${it.damage}" }) +
                ", retaliation ${if (decision.retaliated) decision.attackerDamage else 0}"
    )

    return AttackOutcome(
        miss = decision.miss,
        crit = decision.crit,
        splashHit = decision.splash.isNotEmpty(),
        retaliated = decision.retaliated,
    )
}
