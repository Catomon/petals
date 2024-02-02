package ctmn.petals.unit.abilities

import ctmn.petals.GameConst
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

class HealthPotionAbility : Ability(
    "health_potion",
    Target.ALLY_UNIT,
    10,
    5,
    1,
    0,
) {

    private val baseHealing = 30
    private val healing: Int get() = baseHealing + 15 * level

    override fun activate(playScreen: PlayScreen, unitCaster: UnitActor, tileX: Int, tileY: Int): Boolean {
        val actors = getTargets(playScreen.playStage, unitCaster, tileX, tileY)

        val unit = actors.first() as UnitActor

        if (unit.health >= unit.unitComponent.baseHealth)
            return false

        unit.health += healing
        unit.playStage.addActor(HealthChangeEffect(unit, healing))

        unit.health = minOf(unit.health, unit.unitComponent.baseHealth)

        val effect = HealingEffect(getPositionsArray(actors), playScreen.assets)
        effect.setPosByCenter(
            tileX.unTiled() + GameConst.TILE_SIZE / 2,
            tileY.unTiled() + GameConst.TILE_SIZE / 2
        )
        playScreen.playStage.addActor(effect)

        castTime = effect.lifeTime

        return true
    }
}
