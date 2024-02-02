package ctmn.petals.unit.abilities

import ctmn.petals.GameConst
import ctmn.petals.effects.HealingEffect
import ctmn.petals.playscreen.PlayScreen
import ctmn.petals.unit.UnitActor
import ctmn.petals.playstage.getPositionsArray
import ctmn.petals.unit.Ability
import ctmn.petals.unit.health
import ctmn.petals.unit.heal
import ctmn.petals.utils.setPosByCenter
import ctmn.petals.utils.unTiled

class HealingAbility : Ability(
    "healing",
    Target.MY_TEAM_UNIT,
    2,
    5,
    3,
    0,
) {

    private val healing: Int get() = 50 + 25 * level

    override fun activate(playScreen: PlayScreen, unitCaster: UnitActor, tileX: Int, tileY: Int): Boolean {
        val actors = getTargets(playScreen.playStage, unitCaster, tileX, tileY)

        for (actor in actors) {
            val unit = actor as UnitActor

            if (unit.health >= unit.unitComponent.baseHealth)
                continue

            unit.heal(healing)
        }

        val effect = HealingEffect(getPositionsArray(actors), playScreen.assets)
        effect.setPosByCenter(
            tileX.unTiled() + GameConst.TILE_SIZE / 2,
            tileY.unTiled() + GameConst.TILE_SIZE / 2)
        playScreen.playStage.addActor(effect)

        castTime = effect.lifeTime

        return true
    }
}
