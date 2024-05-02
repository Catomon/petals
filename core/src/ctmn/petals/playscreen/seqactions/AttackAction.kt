package ctmn.petals.playscreen.seqactions

import ctmn.petals.Const
import ctmn.petals.effects.UnitAttackEffect
import ctmn.petals.effects.UnitShakeAction
import ctmn.petals.playscreen.PlayScreen
import ctmn.petals.unit.UnitActor
import ctmn.petals.unit.cAnimationView
import ctmn.petals.unit.tiledX

class AttackAction(
    val attackerUnit: UnitActor,
    val targetUnit: UnitActor,
    val targetDealDamage: () -> Unit,
    val attackerDealDamage: (() -> Unit)?,
    val postAttack: () -> Unit
) : SeqAction() {

    private lateinit var attackEffect: UnitAttackEffect
    private var targetIsShaking = false

    override fun update(deltaTime: Float) {
        isDone = attackEffect.lifeTime <= 0

        if (!isDone) {
            if (attackerUnit.cAnimationView?.animation == attackerUnit.attackAnimation) {
                if (attackerUnit.cAnimationView!!.animation.stateTime >= attackerUnit.cAnimationView!!.animation.animationDuration)
                    if (!targetIsShaking) {
                        targetUnit.addAction(UnitShakeAction(Const.UNIT_SHAKE_POWER, Const.UNIT_SHAKE_DURATION))
                        targetIsShaking = true
                    }
            }
        }
    }

    override fun onStart(playScreen: PlayScreen): Boolean {
        targetDealDamage()
        attackerUnit.setAnimation(attackerUnit.attackAnimation)

        if (attackerDealDamage != null) {
            attackerDealDamage?.invoke()
            targetUnit.setAnimation(targetUnit.attackAnimation)
        }

        attackerUnit.viewComponent.flipX = targetUnit.tiledX < attackerUnit.tiledX

        attackEffect = UnitAttackEffect(playScreen.assets)
        attackEffect.x = targetUnit.x + Const.TILE_SIZE / 2
        attackEffect.y = targetUnit.y + Const.TILE_SIZE / 2
        playScreen.playStage.addActor(attackEffect)

        return true
    }
}