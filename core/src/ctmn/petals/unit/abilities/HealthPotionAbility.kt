package ctmn.petals.unit.abilities

import ctmn.petals.Config
import ctmn.petals.effects.HealingEffect
import ctmn.petals.playstage.getPositionsArray
import ctmn.petals.sim.Battle
import ctmn.petals.utils.setPosByCenter
import ctmn.petals.unit.Ability
import ctmn.petals.unit.UnitActor
import ctmn.petals.unit.health

class HealthPotionAbility : Ability(
    "health_potion",
    Target.ALLY_UNIT,
    2,
    5,
    1,
    0,
) {

    private val baseHealing = 40
    private val healing: Int get() = baseHealing + 20 * level

    override fun activate(battle: Battle, unitCaster: UnitActor, tileX: Int, tileY: Int): Boolean {
        val actors = getTargets(battle.field, unitCaster, tileX, tileY)

        val unit = actors.first() as UnitActor

        if (unit.health >= unit.unitComponent.baseHealth)
            return false

        unit.health += healing
        battle.showHealthChange(unit, healing)

        unit.health = minOf(unit.health, unit.unitComponent.baseHealth)

        val effect = HealingEffect(getPositionsArray(actors), ctmn.petals.assets)
        effect.setPosByCenter(
            tileX * Config.TILE_SIZE + Config.TILE_SIZE / 2,
            tileY * Config.TILE_SIZE + Config.TILE_SIZE / 2
        )
        battle.queueSeqAction(effect)

        castTime = effect.lifeTime

        return true
    }
}
