package ctmn.petals.unit.abilities

import com.badlogic.gdx.graphics.Color
import ctmn.petals.Const
import ctmn.petals.effects.HealingEffect
import ctmn.petals.effects.HealthChangeEffect
import ctmn.petals.playscreen.PlayScreen
import ctmn.petals.playstage.getPositionsArray
import ctmn.petals.playscreen.playStage
import ctmn.petals.unit.Ability
import ctmn.petals.unit.UnitActor
import ctmn.petals.unit.component.BarrierComponent
import ctmn.petals.utils.setPosByCenter
import ctmn.petals.utils.unTiled

class PersonalBarrierAbility : Ability(
    "personal_barrier",
    Target.ALLY_UNIT,
    5,
    5,
    4,
    0,
) {

    private val baseAmount = 25
    private val amount: Int get() = baseAmount + 5 * level

    override fun activate(playScreen: PlayScreen, unitCaster: UnitActor, tileX: Int, tileY: Int): Boolean {
        val actors = getTargets(playScreen.playStage, unitCaster, tileX, tileY)

        for (actor in actors) {
            val unit = actor as UnitActor

            unit.add(BarrierComponent(amount))

            unit.playStage.addActor(HealthChangeEffect(unit, amount).apply { color = Color.YELLOW })
        }

        val effect = HealingEffect(getPositionsArray(actors), playScreen.assets) //todo barrier effect
        effect.setPosByCenter(
            tileX.unTiled() + Const.TILE_SIZE / 2,
            tileY.unTiled() + Const.TILE_SIZE / 2)
        playScreen.playStage.addActor(effect)

        castTime = effect.lifeTime

        return true
    }
}
