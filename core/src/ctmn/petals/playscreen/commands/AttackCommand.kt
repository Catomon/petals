package ctmn.petals.playscreen.commands

import com.badlogic.gdx.Gdx
import ctmn.petals.Config
import ctmn.petals.playscreen.seqactions.AttackAction
import ctmn.petals.playscreen.stageName
import ctmn.petals.sim.Battle
import ctmn.petals.sim.AttackDecision
import ctmn.petals.sim.applyAttack
import ctmn.petals.sim.decideAttack
import ctmn.petals.unit.UnitActor
import ctmn.petals.unit.actionPoints
import ctmn.petals.unit.canAttackNow
import ctmn.petals.unit.inAttackRange
import ctmn.petals.unit.isAlly
import ctmn.petals.unit.playerId
import ctmn.petals.unit.tiledX
import ctmn.petals.unit.tiledY

class AttackCommand(val attackerUnitId: String, val targetUnitId: String) : Command() {

    constructor(attacker: UnitActor, target: UnitActor) : this(attacker.stageName, target.stageName)

    /**
     * What the attack came to. The server decides it when it runs the command, and it travels
     * with the command, so every client applies the same damage and the same deaths instead of
     * rolling its own.
     */
    var decision: AttackDecision? = null

    /** Damage, misses and crits are the server's to decide. */
    override val serverDecides: Boolean get() = true

    override fun explainFailure(battle: Battle): String? {
        val attackerUnit = battle.field.findUnit(attackerUnitId)
            ?: return "no unit '$attackerUnitId' on the field"
        val targetUnit = battle.field.findUnit(targetUnitId)
            ?: return "no unit '$targetUnitId' on the field"

        if (attackerUnit.isAlly(targetUnit)) return "the two units are on the same side"
        if (attackerUnit.actionPoints < Config.ACTION_POINTS_ATTACK_MIN)
            return "unit $attackerUnitId has ${attackerUnit.actionPoints} action points left"
        if (!attackerUnit.inAttackRange(targetUnit.tiledX, targetUnit.tiledY))
            return "unit $targetUnitId is out of range of $attackerUnitId"
        if (!attackerUnit.canAttackNow(targetUnit))
            return "unit $attackerUnitId cannot attack $targetUnitId"

        return null
    }

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

        // Decide once, then apply. A client arrives with the decision already made, so it never
        // rolls damage of its own.
        val decided = decision ?: decideAttack(battle, attackerUnit, targetUnit).also { decision = it }

        battle.queueSeqAction(
            AttackAction(attackerUnit, targetUnit, applyAttack(battle, attackerUnit, targetUnit, decided))
        )

        return true
    }
}
