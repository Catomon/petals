package ctmn.petals.unit.abilities

import com.badlogic.gdx.utils.Array
import ctmn.petals.sim.Battle
import ctmn.petals.unit.*
import ctmn.petals.utils.*
import ctmn.petals.utils.tiledX

class LightningAbility : Ability(
    "lightning",
    Target.ENEMY_UNIT,
    5,
    5,
    4,
    4,
) {

    private var damage: Int = 15 + 5 * level

    override fun activate(battle: Battle, unitCaster: UnitActor, tileX: Int, tileY: Int): Boolean {
        val actors = getTargets(battle.field, unitCaster, tileX, tileY)

        val lightedUnits = Array<UnitActor>()
        actors.firstOrNull { it is UnitActor && it.tiledX == tileX && it.tiledY == tileY }?.let {
            lightedUnits.add(it as UnitActor)
        } ?: return false

        var repeat = true
        while (repeat) {
            repeat = false

            for (unit in lightedUnits) {
                val surroundingUnits = battle.field.getSurroundingUnits(unit).filter { actors.contains(it) }
                val notLighted = surroundingUnits.filterNot { lightedUnits.contains(it, false) }
                if (notLighted.isEmpty()) continue

                notLighted.forEach { lightedUnits.add(it) }
                repeat = true
            }
        }

        //resolve damage immediately, then only play the effects
        for (unit in lightedUnits) {
            if (!battle.friendlyFire && unit.isAlly(unitCaster)) continue

            unit.dealDamage(damage, unitCaster, battle)
        }

        var lastEffect: ctmn.petals.effects.EffectActor? = null

        for (unit in lightedUnits) {
            val effect =
                if (unit.tiledX == tileX && unit.tiledY == tileY)
                    ctmn.petals.effects.CreateEffect.lightning
                else
                    ctmn.petals.effects.CreateEffect.lightningDot

            effect.setPosition(unit.centerX, unit.centerY)

            if (unit.tiledX != tileX || unit.tiledY != tileY)
                battle.queueSeqAction(effect)
            else
                lastEffect = effect

            if (castTime < effect.lifeTime)
                castTime = effect.lifeTime
        }

        if (lastEffect != null)
            battle.queueSeqAction(lastEffect)

        return true
    }
}
