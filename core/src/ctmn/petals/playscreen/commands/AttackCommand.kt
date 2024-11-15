package ctmn.petals.playscreen.commands

import com.badlogic.gdx.Gdx
import ctmn.petals.Const
import ctmn.petals.playscreen.PlayScreen
import ctmn.petals.playscreen.*
import ctmn.petals.unit.*
import ctmn.petals.playscreen.seqactions.AttackAction
import ctmn.petals.playstage.damageTile
import ctmn.petals.unit.UnitActor
import ctmn.petals.unit.component.ATTACK_TYPE_GROUND
import ctmn.petals.unit.component.InvisibilityComponent
import ctmn.petals.unit.component.MoveAfterAttackComponent
import ctmn.petals.unit.component.ReloadingComponent
import ctmn.petals.utils.getSurroundingUnits
import ctmn.petals.utils.getTile
import ctmn.petals.utils.getUnitsInRange
import kotlin.math.max
import kotlin.math.min
import kotlin.random.Random

class AttackCommand(val attackerUnitId: String, val targetUnitId: String) : Command() {

    constructor(attacker: UnitActor, target: UnitActor) : this(attacker.stageName, target.stageName)

    override fun canExecute(playScreen: PlayScreen): Boolean {
        val attackerUnit: UnitActor = playScreen.playStage.root.findActor(attackerUnitId) ?: let {
            Gdx.app.error(this::class.simpleName, "attackerUnit with id:$targetUnitId is not found")
            return false
        }
        val targetUnit: UnitActor = playScreen.playStage.root.findActor(targetUnitId) ?: let {
            Gdx.app.error(this::class.simpleName, "targetUnit with id:$targetUnitId is not found")
            return false
        }

        if (attackerUnit.isAlly(targetUnit)) return false

        if (!attackerUnit.canAttackNow(targetUnit)) return false

        return attackerUnit.actionPoints >= Const.ACTION_POINTS_ATTACK_MIN
    }

    override fun execute(playScreen: PlayScreen): Boolean {
        val attackerUnit: UnitActor = playScreen.playStage.root.findActor(attackerUnitId)
        val targetUnit: UnitActor = playScreen.playStage.root.findActor(targetUnitId)

        attackerUnit.actionPoints -= Const.ACTION_POINTS_ATTACK

        attackerUnit.del(InvisibilityComponent::class.java)
        targetUnit.del(InvisibilityComponent::class.java)

        /** damage */
//        Gdx.app.debug(AttackCommand::class.simpleName, "Execution: ")

        val attackerPair = damageDefense(attackerUnit, targetUnit, playScreen)
        val defenderPair = damageDefense(targetUnit, attackerUnit, playScreen)

        var attackerDamage = attackerPair.first
        val attackerDefense = attackerPair.second

        var defenderDamage = defenderPair.first
        val defenderDefense = defenderPair.second

        attackerDamage -= defenderDefense //final damage
        if (attackerDamage < 1) attackerDamage = 0 //min damage

        defenderDamage -= attackerDefense //final damage
        if (defenderDamage < 1) defenderDamage = 0 //min damage

        val damageTarget: () -> Unit
        var damageAttacker: (() -> Unit)? = null
        val postAttack: () -> Unit

        val miss = if (Const.MISSES) Random.nextInt(1, 101) <= 5 else false
        val crit = if (Const.CRITS) Random.nextInt(1, 101) <= 5 else false

        /** health */
        damageTarget = {
            val attackerDamage: Int = if (crit) (attackerDamage * 1.5f).toInt() else attackerDamage
            targetUnit.dealDamage(attackerDamage, attackerUnit, playScreen, false)

            val isUnitDefenderDie = targetUnit.health <= 0
            if (isUnitDefenderDie) targetUnit.killedBy(attackerUnit, playScreen)

            //splash damage
            val attackerAttackC = attackerUnit.cAttack
            if (attackerAttackC != null && attackerAttackC.attackSplashRange > 0) {
                val unitsInRange = playScreen.playStage.getUnitsInRange(
                    targetUnit.tiledX,
                    targetUnit.tiledY,
                    attackerAttackC.attackSplashRange
                )
                unitsInRange.forEach {
                    //splash damage will not apply if canAttack is false
                    if ((it.tiledX != targetUnit.tiledX || it.tiledY != targetUnit.tiledY) && !it.isAlly(attackerUnit)
                        && attackerUnit.canAttack(it)
                    ) {
                        it.dealDamage(
                            // randomize splash damage down to -5 (min damage is 1)
                            (max(
                                Random.nextInt(
                                    attackerAttackC.attackSplashDamage - 5,
                                    attackerAttackC.attackSplashDamage
                                ), 1
                            ) * attackerUnit.combatDamageHpMod).toInt(),
                            attackerUnit,
                            playScreen,
                            true
                        )
                    }
                }
            }
        }

        //take damage if in target attack range
        val attackTypeNotOk = targetUnit.isAir && targetUnit.cAttack?.attackType == ATTACK_TYPE_GROUND
        if (Const.ATTACK_BACK
            && targetUnit.attackRange > 0
            && !attackTypeNotOk
            && targetUnit.canAttack(attackerUnit)
            && attackerUnit.isUnitNear(targetUnit, 1)
            && targetUnit.cAttack!!.attackRangeBlocked <= 0
            && ((!attackerUnit.isAir || (attackerUnit.isAir && targetUnit.isAir)) || targetUnit.attackRange > 1)
        ) {
            damageAttacker = {
                attackerUnit.dealDamage(defenderDamage, targetUnit, playScreen, false)

                val isUnitAttackerDie = attackerUnit.health <= 0
                if (isUnitAttackerDie) attackerUnit.killedBy(targetUnit, playScreen)

                targetUnit.get(ReloadingComponent::class.java)?.apply {
                    currentTurns = turns
                }
            }
        }

        postAttack = {
            attackerUnit.get(ReloadingComponent::class.java)?.apply {
                currentTurns = turns
            }

            attackerUnit.get(MoveAfterAttackComponent::class.java)?.apply {
                if (!attacked) {
                    attackerUnit.actionPoints = Const.ACTION_POINTS
                    attackerUnit.cUnit.movingRange = secondRange
                    attacked = true
                }
            }

            playScreen.playStage.getTile(targetUnit.tiledX, targetUnit.tiledY)?.let { tile ->
                playScreen.playStage.damageTile(tile, attackerUnit.cAttack?.environmentDmg ?: 0)
            }
        }

        /** add attack action */
        playScreen.queueAction(AttackAction(attackerUnit, targetUnit, damageTarget, damageAttacker, postAttack, miss))

        return true
    }

    private fun damageDefense(unit: UnitActor, vsUnit: UnitActor, playScreen: PlayScreen): Pair<Int, Int> {
        return playScreen.calculateDmgDef(unit, vsUnit)
    }
}
