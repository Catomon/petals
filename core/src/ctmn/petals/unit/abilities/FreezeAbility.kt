package ctmn.petals.unit.abilities

import ctmn.petals.Config
import ctmn.petals.sim.Battle
import ctmn.petals.unit.*
import ctmn.petals.unit.UnitActor
import ctmn.petals.unit.buff.Buff
import ctmn.petals.utils.setPosByCenter

class FreezeAbility : Ability(
    "freeze",
    Target.ENEMY_UNIT,
    6,
    5,
    3,
    1,
) {

    override fun activate(battle: Battle, unitCaster: UnitActor, tileX: Int, tileY: Int): Boolean {
        val actors = getTargets(battle.field, unitCaster, tileX, tileY)

        for (actor in actors) {
            val unit = if (actor is UnitActor) actor else continue //actor as UnitActor

            if (!battle.friendlyFire && actor.isAlly(unitCaster)) continue

            unit.buffs.add(Buff("freeze", duration = 1f))
            unit.actionPoints = 0
            unit.updateView()
        }

        val effect = ctmn.petals.effects.CreateEffect.freeze
        effect.setPosByCenter(
            tileX * Config.TILE_SIZE + Config.TILE_SIZE / 2,
            tileY * Config.TILE_SIZE + Config.TILE_SIZE / 2)
        battle.queueSeqAction(effect)

        castTime = effect.lifeTime

        return true
    }
}
