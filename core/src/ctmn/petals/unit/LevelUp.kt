package ctmn.petals.unit

import ctmn.petals.GameConst.MAX_LVL
import ctmn.petals.story.alissa.CreateUnit
import ctmn.petals.unit.UnitIds.DOLL_AXE
import ctmn.petals.unit.UnitIds.DOLL_BOW
import ctmn.petals.unit.UnitIds.DOLL_PIKE
import ctmn.petals.unit.UnitIds.DOLL_SWORD
import ctmn.petals.unit.abilities.SummonAbility
import ctmn.petals.unit.actors.Alice
import kotlin.math.ceil
import kotlin.math.max

//TODO

fun UnitActor.levelUp() {
    val up = when (this) {
        is Alice -> StoryAliceLevelUp
        else -> UnitLevelUp
    }

    up.apply(this)
}

abstract class LevelUp<U : UnitActor> {

    abstract fun apply(unit: @UnsafeVariance U)
}

object StoryAliceLevelUp : LevelUp<Alice>() {

    val manaPerLvl = 10

    override fun apply(unit: Alice) {
        with (unit) {
            val cLevel = cLevel ?: return
            val lvl = cLevel.lvl

            val unitBase = CreateUnit.alice

            cAttack?.apply {
                minDamage = unitBase.cAttack!!.minDamage + cLevel.dmgPerLvl * (lvl - 1)
                maxDamage = unitBase.cAttack!!.maxDamage + cLevel.dmgPerLvl * (lvl - 1)
                defense = unitBase.defense + cLevel.dfPerLvl * (lvl - 1)
            }

            if (cAbilities != null) {
                for (ability in cAbilities!!.abilities) {
                    ability.cooldown = ceil((MAX_LVL + 1 - lvl).toFloat() / 2f).toInt()
                }

                if (lvl > 1 && mana == cAbilities!!.baseMana + (lvl - 2) * manaPerLvl)
                    mana = cAbilities!!.baseMana + (lvl - 1) * manaPerLvl
            }

            val summonAbility = cAbilities?.abilities?.firstOrNull { it is SummonAbility } as SummonAbility?
            if (summonAbility != null) {
                if (lvl >= 1) summonAbility.cooldown = 4
                if (lvl >= 2) summonAbility.cooldown = 4
                if (lvl >= 3) summonAbility.cooldown = 3
                if (lvl >= 4) summonAbility.cooldown = 3
                if (lvl >= 5) summonAbility.cooldown = 2
                if (lvl >= 6) summonAbility.cooldown = 2
                if (lvl >= 7) summonAbility.cooldown = 1


                if (lvl >= 1) summoner.units.add(DOLL_PIKE)
                if (lvl >= 2) summoner.units.add(DOLL_SWORD)
                if (lvl >= 3) summoner.units.add(DOLL_BOW)
                if (lvl >= 4) summoner.units.add(DOLL_AXE)

                when (lvl) {
                    3 -> summoner.maxUnits = max(summoner.maxUnits, 5)
                    5 -> summoner.maxUnits = max(summoner.maxUnits, 6)
                    7 -> summoner.maxUnits = max(summoner.maxUnits, 7)
                    9 -> summoner.maxUnits = max(summoner.maxUnits, 8)
                    10 -> summoner.maxUnits = max(summoner.maxUnits, 8)
                }
            }
        }
    }
}

object UnitLevelUp : LevelUp<UnitActor>() {
    override fun apply(unit: UnitActor) {
        with(unit) {
            val cLevel = cLevel ?: return
            val lvl = cLevel.lvl

            val unitBase = unit.javaClass.constructors.first().newInstance() as UnitActor

            cAttack?.apply {
                minDamage = unitBase.cAttack!!.minDamage + cLevel.dmgPerLvl * (lvl - 1)
                maxDamage = unitBase.cAttack!!.maxDamage + cLevel.dmgPerLvl * (lvl - 1)
                defense = unitBase.defense + cLevel.dfPerLvl * (lvl - 1)
            }
        }
    }
}