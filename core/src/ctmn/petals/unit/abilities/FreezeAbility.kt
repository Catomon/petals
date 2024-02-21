package ctmn.petals.unit.abilities

import ctmn.petals.Const
import ctmn.petals.playscreen.PlayScreen
import ctmn.petals.unit.*
import ctmn.petals.unit.UnitActor
import ctmn.petals.unit.buff.Buff
import ctmn.petals.utils.setPosByCenter
import ctmn.petals.utils.unTiled

class FreezeAbility : Ability(
    "freeze",
    Target.ENEMY_UNIT,
    6,
    5,
    3,
    1,
) {

    override fun activate(playScreen: PlayScreen, unitCaster: UnitActor, tileX: Int, tileY: Int): Boolean {
        val playStage = playScreen.playStage

        val actors = getTargets(playScreen.playStage, unitCaster, tileX, tileY)

        for (actor in actors) {
            val unit = if (actor is UnitActor) actor else continue //actor as UnitActor

            if (!playScreen.friendlyFire && actor.isAlly(unitCaster)) continue

            unit.buffs.add(Buff("freeze", duration = 1f))
            unit.actionPoints = 0
            unit.updateView()
        }

        val effect = ctmn.petals.effects.CreateEffect.freeze
        effect.setPosByCenter(
            tileX.unTiled() + Const.TILE_SIZE / 2,
            tileY.unTiled() + Const.TILE_SIZE / 2)
        playStage.addActor(effect)

        castTime = effect.lifeTime

        return true
    }
}
