package ctmn.petals.unit

import ctmn.petals.playscreen.commands.AttackCommand
import ctmn.petals.playscreen.selfName
import ctmn.petals.playstage.getLeadUnit
import ctmn.petals.playstage.getUnitsForTeam
import ctmn.petals.sim.Battle
import ctmn.petals.unit.component.BonusFieldComponent
import ctmn.petals.utils.GameLog
import java.lang.IllegalStateException

/**
 * A damage roll between the two bounds, inclusive.
 *
 * The bounds are ordered first. A debuff such as exhaust can push a unit's damage below zero, and
 * a span still has a low and a high end; without this the roll asks for an empty range and throws,
 * which is what killed a client mid match.
 */
fun Battle.randomInt(min: Int, max: Int): Int {
    val low = minOf(min, max)
    val high = maxOf(max, min)

    return random.nextInt(low, high + 1)
}

/** A roll in the upper part of `0..value`. Nothing to roll for a value that is already gone. */
fun Battle.randomInt75(value: Int): Int =
    if (value <= 0) 0 else random.nextInt((value * 0.75f).toInt(), value + 1)

fun Battle.randomInt50(value: Int): Int =
    if (value <= 0) 0 else random.nextInt((value * 0.75f).toInt(), value + 1)

fun Battle.randomInt25(value: Int): Int =
    if (value <= 0) 0 else random.nextInt((value * 0.75f).toInt(), value + 1)

/** up to 50% damage and 25% defense reducing depending on health */
val UnitActor.combatDamageHpMod: Float
    get() {
        val modifierHealth: Float = health.toFloat() / unitComponent.baseHealth.toFloat()
        return modifierHealth / 2 + 0.5f
    }

/** up to 50% damage and 25% defense reducing depending on health */
val UnitActor.combatDefenseHpMod: Float
    get() {
        val modifierHealth: Float = health.toFloat() / unitComponent.baseHealth.toFloat()
        return modifierHealth / 4 + 0.75f
    }

/** Final combat stats before the damage roll. */
data class DamageDefense(
    val minDamage: Int,
    val maxDamage: Int,
    val defense: Int,
)

/**
 * Deterministic damage and defense for [unit] against [vsUnit], after terrain, matchup,
 * leader and buff modifiers. The AI evaluates with this so "thinking" never consumes the
 * game RNG.
 */
fun Battle.calculateDmgDefStats(unit: UnitActor, vsUnit: UnitActor): DamageDefense {
    val battleField = field

    var defense = unit.defense

    var minDMG = unit.minDamage
    var maxDMG = unit.maxDamage

    //terrain
    val tileActor = battleField.getTile(unit.tiledX, unit.tiledY)
        ?: throw IllegalStateException("Tile not fount at ${unit.tiledX}:${unit.tiledY}")

    if (unit.cTerrainProps == null) GameLog.error(AttackCommand::class.simpleName ?: "AttackCommand", "Unit has not terrain props")

    val attackBuff = if (tileActor == null) 0 else unit.cTerrainProps?.get(tileActor.terrain)?.attackBonus ?: 0
    val defenseBuff = if (tileActor == null) 0 else unit.cTerrainProps?.get(tileActor.terrain)?.defenseBonus ?: 0

    minDMG += attackBuff
    maxDMG += attackBuff
    defense += defenseBuff

    //bonus fields
    battleField.getUnitsForTeam(unit.playerId).mapNotNull {
        if (it == unit)
            null
        else
            it.get(BonusFieldComponent::class.java)
    }.forEach {
        minDMG += it.damage
        maxDMG += it.damage
        defense += it.defense
    }

    //matchup
    val matchupBonusDmgDefPair = unit.cMatchUp?.get(vsUnit.selfName)
    matchupBonusDmgDefPair?.apply {
        minDMG += first
        maxDMG += first
        defense += second
        GameLog.verbose(
            "AttackCommand",
            "${unit.name} matchup bonus: A${first}; D${second}"
        )
    }

    //leader buff
    if (unit.isFollower) {
        val leader = battleField.getLeadUnit(unit.followerID) //followerID == leaderID
        if (leader != null) {
            val leaderC = leader.cLeader!!
            if (unit.isUnitNear(leader, leaderC.leaderRange)) {
                minDMG += leaderC.leaderDmgBuff
                maxDMG += leaderC.leaderDmgBuff
                defense += leaderC.leaderDefBuff
            }
        }
    }

    //buffs
    for (buff in unit.buffs) {
        when (buff.name) {
            "attack" -> {
                minDMG += buff.value
                maxDMG += buff.value

                minDMG = (minDMG * buff.coefficient).toInt()
                maxDMG = (maxDMG * buff.coefficient).toInt()
            }

            "defense" -> {
                defense += buff.value
                defense = (defense * buff.coefficient).toInt()
            }
        }
    }

    val damageMod = unit.combatDamageHpMod
    val defenseMod = unit.combatDefenseHpMod

    minDMG = (minDMG * damageMod).toInt()
    maxDMG = (maxDMG * damageMod).toInt()

    defense = (defense * defenseMod).toInt()

    // Exhaust and other debuffs can take a unit's damage below zero, and a range that reads
    // backwards is not a range. Order the pair here, where it is produced, so everything that
    // reads it (the roll, the AI's estimate) sees a low and a high end.
    return DamageDefense(minOf(minDMG, maxDMG), maxOf(minDMG, maxDMG), defense)
}

/** Rolls the damage for one attack. Uses the game RNG on purpose. */
fun Battle.calculateDmgDef(unit: UnitActor, vsUnit: UnitActor): Pair<Int, Int> {
    val stats = calculateDmgDefStats(unit, vsUnit)
    return randomInt(stats.minDamage, stats.maxDamage) to stats.defense
}
