package ctmn.petals.playscreen.commands

import com.badlogic.gdx.Gdx
import ctmn.petals.Config
import ctmn.petals.playscreen.seqactions.AttackAction
import ctmn.petals.playscreen.stageName
import ctmn.petals.sim.Battle
import ctmn.petals.sim.resolveAttack
import ctmn.petals.unit.UnitActor
import ctmn.petals.unit.actionPoints
import ctmn.petals.unit.canAttackNow
import ctmn.petals.unit.isAlly
import ctmn.petals.unit.playerId

class AttackCommand(val attackerUnitId: String, val targetUnitId: String) : Command() {

    constructor(attacker: UnitActor, target: UnitActor) : this(attacker.stageName, target.stageName)

    /** Damage, misses and crits are rolled, so the server has to run this one. */
    override val rollsDice: Boolean get() = true

    override fun canExecute(battle: Battle): Boolean {
        val attackerUnit: UnitActor = battle.field.findUnit(attackerUnitId) ?: let {
            Gdx.app.error(this::class.simpleName, "attackerUnit with id:$targetUnitId is not found")
            return false
        }
        val targetUnit: UnitActor = battle.field.findUnit(targetUnitId) ?: let {
            Gdx.app.error(this::class.simpleName, "targetUnit with id:$targetUnitId is not found")
            return false
        }

        if (attackerUnit.isAlly(targetUnit)) return false

        if (!attackerUnit.canAttackNow(targetUnit)) return false

        return attackerUnit.actionPoints >= Config.ACTION_POINTS_ATTACK_MIN
    }

    override fun execute(battle: Battle): Boolean {
        val attackerUnit: UnitActor = battle.field.findUnit(attackerUnitId) ?: return false
        val targetUnit: UnitActor = battle.field.findUnit(targetUnitId) ?: return false

        // State and randomness are resolved up front; the animation only plays it back.
        val outcome = resolveAttack(battle, attackerUnit, targetUnit)

        battle.queueSeqAction(AttackAction(attackerUnit, targetUnit, outcome))

        return true
    }
}
