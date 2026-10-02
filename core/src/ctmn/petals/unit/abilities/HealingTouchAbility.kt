package ctmn.petals.unit.abilities

import ctmn.petals.Config
import ctmn.petals.effects.HealingEffect
import ctmn.petals.hasAssets
import ctmn.petals.playstage.getPositionsArray
import ctmn.petals.sim.Battle
import ctmn.petals.utils.setPosByCenter
import ctmn.petals.unit.Ability
import ctmn.petals.unit.UnitActor
import ctmn.petals.unit.health

class HealingTouchAbility : Ability(
    "healing_touch",
    Target.ALLY_UNIT,
    3,
    5,
    2,
    0,
) {

    private val baseHealing = 50
    private val healing: Int get() = baseHealing + 25 * level

    override fun activate(battle: Battle, unitCaster: UnitActor, tileX: Int, tileY: Int): Boolean {
        val actors = getTargets(battle.field, unitCaster, tileX, tileY)

        for (actor in actors) {
            val unit = actor as UnitActor

            if (unit.health >= unit.unitComponent.baseHealth)
                continue

            unit.health += healing
            battle.showHealthChange(unit, healing)
            if (unit.health > unit.unitComponent.baseHealth)
                unit.health = unit.unitComponent.baseHealth
        }

        // Visual only, and it reads the atlas, so a headless battle skips it.
        if (hasAssets) {
            val effect = HealingEffect(getPositionsArray(actors), ctmn.petals.assets)
            effect.setPosByCenter(
                tileX * Config.TILE_SIZE + Config.TILE_SIZE / 2,
                tileY * Config.TILE_SIZE + Config.TILE_SIZE / 2)
            battle.queueSeqAction(effect)

            castTime = effect.lifeTime
        }

        return true
    }
}
