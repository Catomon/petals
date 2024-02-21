package ctmn.petals.playscreen.seqactions

import ctmn.petals.Const
import ctmn.petals.effects.UnitAttackEffect
import ctmn.petals.effects.UnitShakeAction
import ctmn.petals.playscreen.PlayScreen
import ctmn.petals.unit.UnitActor

class AttackAction(val attackerUnit: UnitActor, val targetUnit: UnitActor) : SeqAction() {

    private lateinit var attackEffect: UnitAttackEffect

    override fun update(deltaTime: Float) {
        isDone = attackEffect.lifeTime <= 0
    }

    override fun onStart(playScreen: PlayScreen): Boolean {
        attackEffect = UnitAttackEffect(playScreen.assets)
        attackEffect.x = targetUnit.x  + Const.TILE_SIZE / 2
        attackEffect.y = targetUnit.y + Const.TILE_SIZE / 2
        playScreen.playStage.addActor(attackEffect)

        targetUnit.addAction(UnitShakeAction(Const.UNIT_SHAKE_POWER, Const.UNIT_SHAKE_DURATION))

        return true
    }
}