package ctmn.petals.playscreen.seqactions

import ctmn.petals.Const
import ctmn.petals.effects.MissileActor
import ctmn.petals.effects.UnitAttackEffect
import ctmn.petals.effects.UnitShakeAction
import ctmn.petals.playscreen.PlayScreen
import ctmn.petals.unit.UnitActor
import ctmn.petals.unit.cAnimationView
import ctmn.petals.unit.tiledX
import ctmn.petals.utils.centerX
import ctmn.petals.utils.centerY

class AttackAction(
    val attackerUnit: UnitActor,
    val targetUnit: UnitActor,
    val damageTarget: () -> Unit,
    val damageAttacker: (() -> Unit)?,
    val postAttack: () -> Unit,
) : SeqAction() {

    private lateinit var attackEffect: UnitAttackEffect
    private var targetIsShaking = false

    private var attackerAttacked = false
    private var defenderAttacked = false

    private var attackMissile: MissileActor? = null

    override fun update(deltaTime: Float) {
        if (attackMissile != null) {
            if (attackMissile!!.stage == null)
                attackMissile = null
            else {
                when (attackerUnit.animationProps.attackEffectFrame) {
                    0f -> if (attackMissile!!.isLanded) attackMissile = null
                    0.5f -> if (attackerUnit.animationProps.attackEffectFrame <= attackMissile!!.explosionSprite.animation.progressLast) attackMissile =
                        null
                }
            }
        }

        if (!attackerAttacked) {
            if ((attackerUnit.animationProps.attackFrame <= (attackerUnit.attackAnimation?.progressLast
                    ?: 0f) || attackerUnit.cAnimationView?.animation != attackerUnit.attackAnimation
                        ) && attackMissile == null
            ) {

                if (!targetIsShaking) {
                    targetUnit.addAction(UnitShakeAction(Const.UNIT_SHAKE_POWER, Const.UNIT_SHAKE_DURATION))
                    targetIsShaking = true
                }

                damageTarget()

                attackEffect = UnitAttackEffect(playScreen.assets)
                attackEffect.x = targetUnit.x + Const.TILE_SIZE / 2
                attackEffect.y = targetUnit.y + Const.TILE_SIZE / 2
                playScreen.playStage.addActor(attackEffect)

                attackerAttacked = true
            }
        }

        if (!defenderAttacked) {
            if (targetUnit.animationProps.attackFrame <= (targetUnit.attackAnimation?.stateTime
                    ?: 0f) || targetUnit.cAnimationView?.animation != targetUnit.attackAnimation
            ) {
                damageAttacker?.invoke()

                defenderAttacked = true
            }
        }

        isDone = attackerAttacked && defenderAttacked

//        if (!isDone) {
//            if (attackerUnit.cAnimationView?.animation == attackerUnit.attackAnimation) {
//                if (attackerUnit.cAnimationView!!.animation.stateTime >= attackerUnit.cAnimationView!!.animation.animationDuration)
//                    if (!targetIsShaking) {
//                        targetUnit.addAction(UnitShakeAction(Const.UNIT_SHAKE_POWER, Const.UNIT_SHAKE_DURATION))
//                        targetIsShaking = true
//                    }
//            }
//        }
    }

    override fun onStart(playScreen: PlayScreen): Boolean {
        //
        attackerUnit.setAnimation(attackerUnit.attackAnimation)

        if (damageAttacker != null) {
            //
            targetUnit.setAnimation(targetUnit.attackAnimation)
        } else {
            defenderAttacked = true
        }

        attackerUnit.viewComponent.flipX = targetUnit.tiledX < attackerUnit.tiledX

        if (attackerUnit.attackEffect != null) {
            attackMissile = attackerUnit.attackEffect
            val attackMissile = attackMissile as MissileActor
            //attackMissile.setPosition(attackerUnit.centerX, attackerUnit.centerY)
            attackMissile.setTarget(targetUnit.centerX, targetUnit.centerY)
            playScreen.playStage.addActor(attackMissile)
        }

        return true
    }
}