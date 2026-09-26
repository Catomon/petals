package ctmn.petals.unit.abilities

import ctmn.petals.Config
import ctmn.petals.effects.CreateEffect
import ctmn.petals.sim.Battle
import ctmn.petals.unit.Ability
import ctmn.petals.unit.UnitActor

class UnsummonAbility : Ability(
    "unsummon",
    Target.MY_UNIT,
    1,
    5,
    0,
    0,
) {

    override fun activate(battle: Battle, unitCaster: UnitActor, tileX: Int, tileY: Int): Boolean {
        battle.field.removeGameActor(unitCaster)

        //create effect
        val effect = CreateEffect.summon
        effect.setPosition(
            tileX * Config.TILE_SIZE + Config.TILE_SIZE / 2,
            tileY * Config.TILE_SIZE + Config.TILE_SIZE / 2
        )
        battle.queueSeqAction(effect)

        castTime = effect.lifeTime * 0.70f

        return true
    }
}
