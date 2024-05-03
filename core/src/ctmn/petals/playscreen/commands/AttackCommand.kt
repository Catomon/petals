package ctmn.petals.playscreen.commands

import com.badlogic.gdx.Gdx
import ctmn.petals.Const
import ctmn.petals.playscreen.PlayScreen
import ctmn.petals.playscreen.*
import ctmn.petals.unit.*
import ctmn.petals.playscreen.seqactions.AttackAction
import ctmn.petals.unit.UnitActor
import ctmn.petals.unit.component.InvisibilityComponent

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

        if (!attackerUnit.canAttack(targetUnit)) return false

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

        /** health */
        damageTarget = {
            targetUnit.dealDamage(attackerDamage, attackerUnit, playScreen, false)

            val isUnitDefenderDie = targetUnit.health <= 0
            if (isUnitDefenderDie) targetUnit.killedBy(attackerUnit, playScreen)
        }

        //take damage if in target attack range
        if (attackerUnit.isUnitNear(targetUnit, 1)
            && targetUnit.cAttack!!.attackRangeBlocked <= 0
            && ((!attackerUnit.isAir || (attackerUnit.isAir && targetUnit.isAir)) || targetUnit.attackRange > 1)
        ) {
            damageAttacker = {
                attackerUnit.dealDamage(defenderDamage, targetUnit, playScreen, false)

                val isUnitAttackerDie = attackerUnit.health <= 0
                if (isUnitAttackerDie) attackerUnit.killedBy(targetUnit, playScreen)
            }
        }

        postAttack = {
            /** remove unit if dead */
            //moved
        }

        /** add attack action */
        playScreen.queueAction(AttackAction(attackerUnit, targetUnit, damageTarget, damageAttacker, postAttack))

        return true
    }

    private fun damageDefense(unit: UnitActor, vsUnit: UnitActor, playScreen: PlayScreen): Pair<Int, Int> {
        return playScreen.calculateDmgDef(unit, vsUnit)
    }
}
