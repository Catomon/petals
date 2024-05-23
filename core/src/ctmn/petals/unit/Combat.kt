package ctmn.petals.unit

import com.badlogic.gdx.Gdx
import ctmn.petals.playscreen.PlayScreen
import ctmn.petals.playscreen.commands.AttackCommand
import ctmn.petals.playscreen.playStageOrNull
import ctmn.petals.playscreen.selfName
import ctmn.petals.playstage.getLeadUnit
import ctmn.petals.playstage.getUnitsForTeam
import ctmn.petals.unit.component.BonusFieldComponent
import java.lang.IllegalStateException

fun PlayScreen.randomDamage(min: Int, max: Int): Int {
    val modMinDamage: Int = (min.toFloat()).toInt()
    val modMaxDamage: Int = (max.toFloat()).toInt()

    return random.nextInt(modMinDamage, modMaxDamage + 1)
}

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

fun PlayScreen.calculateDmgDef(unit: UnitActor, vsUnit: UnitActor): Pair<Int, Int> {
    val playStage = unit.playStageOrNull

    var defense = unit.defense

    var minDMG = unit.minDamage
    var maxDMG = unit.maxDamage

    //terrain
    val tileActor = playStage?.getTile(unit.tiledX, unit.tiledY)
        ?: if (playStage != null) throw IllegalStateException("Tile not fount at ${unit.tiledX}:${unit.tiledY}") else null

    if (unit.cTerrainProps == null) Gdx.app.error(AttackCommand::class.simpleName, "Unit has not terrain props")

    val attackBuff = if (tileActor == null) 0 else unit.cTerrainProps?.get(tileActor.terrain)?.attackBonus ?: 0
    val defenseBuff = if (tileActor == null) 0 else unit.cTerrainProps?.get(tileActor.terrain)?.defenseBonus ?: 0

    minDMG += attackBuff
    maxDMG += attackBuff
    defense += defenseBuff

    //bonus fields
    playStage?.getUnitsForTeam(unit.playerId)?.mapNotNull {
        if (it == unit)
            null
        else
            it.get(BonusFieldComponent::class.java)
    }?.forEach {
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
        Gdx.app.debug(
            AttackCommand::class.simpleName,
            "${unit.name} matchup bonus: A${first}; D${second}"
        )
    }

    //leader buff
    if (unit.isFollower) {
        val leader = playStage?.getLeadUnit(unit.followerID) //followerID == leaderID
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

    return Pair(randomDamage(minDMG, maxDMG), defense)
}