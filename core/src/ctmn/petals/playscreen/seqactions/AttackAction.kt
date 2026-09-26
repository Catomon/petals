package ctmn.petals.playscreen.seqactions

import com.badlogic.gdx.graphics.Color
import ctmn.petals.AudioManager
import ctmn.petals.Config
import ctmn.petals.effects.FloatingUpLabel
import ctmn.petals.effects.MissileActor
import ctmn.petals.effects.UnitAttackEffect
import ctmn.petals.effects.UnitShakeAction
import ctmn.petals.playscreen.PlayScreen
import ctmn.petals.sim.AttackOutcome
import ctmn.petals.tile.cutGrass
import ctmn.petals.unit.UnitActor
import ctmn.petals.unit.cAnimationView
import ctmn.petals.unit.tiledX
import ctmn.petals.utils.centerX
import ctmn.petals.utils.centerY
import ctmn.petals.utils.getTile
import kotlin.random.Random

/**
 * View-only attack animation. The attack itself was already resolved by `resolveAttack` in
 * the command, so this class never changes game state. It only plays the animation, effects,
 * sound and floating labels for a result that already exists.
 */
class AttackAction(
    val attackerUnit: UnitActor,
    val targetUnit: UnitActor,
    val outcome: AttackOutcome,
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
                if (!outcome.miss) {
                    if (!targetIsShaking) {
                        targetUnit.addAction(UnitShakeAction(Config.UNIT_SHAKE_POWER, Config.UNIT_SHAKE_DURATION))
                        targetIsShaking = true
                    }
                } else {
                    playScreen.playStage.addActor(FloatingUpLabel("Miss!", 15f).also {
                        it.color = Color.RED
                        it.setFontScale(0.33f)
                        it.setPosition(targetUnit.centerX, targetUnit.centerY)
                    })
                }

                attackEffect = UnitAttackEffect(playScreen.assets)
                attackEffect.x = targetUnit.x + Config.TILE_SIZE / 2
                attackEffect.y = targetUnit.y + Config.TILE_SIZE / 2
                playScreen.playStage.addActor(attackEffect)

                playScreen.playStage.getTile(targetUnit)?.cutGrass()

                AudioManager.playSound(attackerUnit.hitSounds.random().replace(".ogg", ""), 1.1f - Random.nextFloat() * 0.20f)

                attackerAttacked = true
            }
        }

        if (!defenderAttacked) {
            defenderAttacked = true
        }

        isDone = attackerAttacked && defenderAttacked
    }

    override fun onStart(playScreen: PlayScreen): Boolean {
        attackerUnit.setAnimation(attackerUnit.attackAnimation)

        if (outcome.retaliated) {
            targetUnit.setAnimation(targetUnit.attackAnimation)
        } else {
            defenderAttacked = true
        }

        attackerUnit.viewComponent.flipX = targetUnit.tiledX < attackerUnit.tiledX

        if (attackerUnit.attackEffect != null) {
            attackMissile = attackerUnit.attackEffect
            val attackMissile = attackMissile as MissileActor
            attackMissile.setTarget(targetUnit.centerX, targetUnit.centerY)
            playScreen.playStage.addActor(attackMissile)
        }

        return true
    }
}
