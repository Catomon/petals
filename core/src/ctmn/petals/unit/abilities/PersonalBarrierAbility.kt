package ctmn.petals.unit.abilities

import com.badlogic.gdx.graphics.Color
import ctmn.petals.Config
import ctmn.petals.effects.HealingEffect
import ctmn.petals.hasAssets
import ctmn.petals.playstage.getPositionsArray
import ctmn.petals.sim.Battle
import ctmn.petals.utils.centerX
import ctmn.petals.utils.centerY
import ctmn.petals.utils.setPosByCenter
import ctmn.petals.unit.Ability
import ctmn.petals.unit.UnitActor
import ctmn.petals.unit.component.BarrierComponent

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

    override fun activate(battle: Battle, unitCaster: UnitActor, tileX: Int, tileY: Int): Boolean {
        val actors = getTargets(battle.field, unitCaster, tileX, tileY)

        for (actor in actors) {
            val unit = actor as UnitActor

            unit.add(BarrierComponent(amount))

            battle.showFloatingLabel("+$amount", Color.YELLOW, unit.centerX, unit.centerY)
        }

        // Visual only, and it reads the atlas, so a headless battle skips it.
        if (hasAssets) {
            val effect = HealingEffect(getPositionsArray(actors), ctmn.petals.assets) //todo barrier effect
            effect.setPosByCenter(
                tileX * Config.TILE_SIZE + Config.TILE_SIZE / 2,
                tileY * Config.TILE_SIZE + Config.TILE_SIZE / 2)
            battle.queueSeqAction(effect)

            castTime = effect.lifeTime
        }

        return true
    }
}
