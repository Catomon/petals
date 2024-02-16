package ctmn.petals.unit

import com.badlogic.gdx.Gdx
import ctmn.petals.playscreen.PlayScreen
import ctmn.petals.playscreen.commands.AttackCommand
import ctmn.petals.playscreen.playStageOrNull
import ctmn.petals.playscreen.selfName
import ctmn.petals.playstage.getLeadUnit
import java.lang.IllegalStateException

fun PlayScreen.randomDamage(min: Int, max: Int): Int {
    val modMinDamage: Int = (min.toFloat()).toInt()
    val modMaxDamage: Int = (max.toFloat()).toInt()

    return random.nextInt(modMinDamage, modMaxDamage + 1)
}

fun PlayScreen.calculateDmgDef(unit: UnitActor, vsUnit: UnitActor): Pair<Int, Int> {
    val playStage = unit.playStageOrNull

    var defense = unit.defense

    var minDMG = unit.minDamage
    var maxDMG = unit.maxDamage

    val tileActor = playStage?.getTile(unit.tiledX, unit.tiledY) ?: if (playStage != null) throw IllegalStateException("Tile not fount at ${unit.tiledX}:${unit.tiledY}") else null
    val attackBuff = if (tileActor == null) 0 else unit.terrainBuff.get(tileActor.terrain)?.first ?: 0.also { Gdx.app.error(AttackCommand::class.simpleName, "A terrain buff not found for $unit at ${tileActor.terrain}") }
    val defenseBuff = if (tileActor == null) 0 else unit.terrainBuff.get(tileActor.terrain)?.second ?: 0.also { Gdx.app.error(AttackCommand::class.simpleName, "A terrain buff not found for $unit at ${tileActor.terrain}") }

    minDMG += attackBuff
    maxDMG += attackBuff
    defense += defenseBuff

    val matchupBonusPair = unit.matchupBonus.get(vsUnit.selfName)
    matchupBonusPair?.apply {
        minDMG += first
        maxDMG += first
        defense += second
        Gdx.app.debug(
            AttackCommand::class.simpleName,
            "${unit.name} matchup bonus: A${first}; D${second}")
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

    //up to 50% damage and 25% defense reducing depending on health
    val modifierHealth: Float = unit.health.toFloat() / unit.unitComponent.baseHealth.toFloat()

    val damageMod = modifierHealth / 2 + 0.5
    val defenseMod = modifierHealth / 4 + 0.75

    minDMG = (minDMG * damageMod).toInt()
    maxDMG = (maxDMG * damageMod).toInt()

    defense = (defense * defenseMod).toInt()

    return Pair(randomDamage(minDMG, maxDMG), defense)
}