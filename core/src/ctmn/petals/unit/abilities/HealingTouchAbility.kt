package ctmn.petals.unit.abilities

import ctmn.petals.Const
import ctmn.petals.effects.HealingEffect
import ctmn.petals.effects.HealthChangeEffect
import ctmn.petals.playscreen.PlayScreen
import ctmn.petals.playstage.getPositionsArray
import ctmn.petals.playscreen.playStage
import ctmn.petals.unit.Ability
import ctmn.petals.unit.UnitActor
import ctmn.petals.unit.health
import ctmn.petals.utils.setPosByCenter
import ctmn.petals.utils.unTiled

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

    override fun activate(playScreen: PlayScreen, unitCaster: UnitActor, tileX: Int, tileY: Int): Boolean {
        val actors = getTargets(playScreen.playStage, unitCaster, tileX, tileY)

        for (actor in actors) {
            val unit = actor as UnitActor

            if (unit.health >= unit.unitComponent.baseHealth)
                continue

            unit.health += healing
            unit.playStage.addActor(HealthChangeEffect(unit, healing))
            if (unit.health > unit.unitComponent.baseHealth)
                unit.health = unit.unitComponent.baseHealth
        }

        val effect = HealingEffect(getPositionsArray(actors), playScreen.assets)
        effect.setPosByCenter(
            tileX.unTiled() + Const.TILE_SIZE / 2,
            tileY.unTiled() + Const.TILE_SIZE / 2)
        playScreen.playStage.addActor(effect)

        castTime = effect.lifeTime

        return true
    }
}
