package ctmn.petals.unit.abilities

import ctmn.petals.playscreen.PlayScreen
import ctmn.petals.unit.Ability
import ctmn.petals.unit.UnitActor
import ctmn.petals.unit.buff.Buff
import ctmn.petals.unit.buffs
import ctmn.petals.utils.centerX
import ctmn.petals.utils.centerY
import ctmn.petals.utils.setPosByCenter

class ExhaustAbility : Ability(
    "exhaust",
    Target.ENEMY_UNIT,
    1,
    1,
    2,
    1,
) {

    private val defense get() = -5 - 5 * level
    private val damage get() = -5 - 5 * level

    override fun activate(playScreen: PlayScreen, unitCaster: UnitActor, tileX: Int, tileY: Int): Boolean {
        val actors = getTargets(playScreen.playStage, unitCaster, tileX, tileY)

        for (actor in actors) {
            if (actor is UnitActor) {
                actor.buffs.add(Buff("defense", defense, 1f, 1f))
                actor.buffs.add(Buff("attack", damage, 1f, 1f))

                val effect = ctmn.petals.effects.CreateEffect.exhaust
                effect.setPosByCenter(actor.centerX, actor.centerY)
                playScreen.playStage.addActor(effect)
                castTime = effect.lifeTime
            }
        }

        return true
    }
}
