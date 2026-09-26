package ctmn.petals.unit.abilities

import com.badlogic.gdx.graphics.Color
import ctmn.petals.Config
import ctmn.petals.effects.CreateEffect
import ctmn.petals.playscreen.seqactions.OneSeqAction
import ctmn.petals.sim.Battle
import ctmn.petals.unit.*
import ctmn.petals.unit.UnitActor
import ctmn.petals.unit.component.TraitComponent
import ctmn.petals.utils.centerX
import ctmn.petals.utils.centerY

class FireboltAbility : Ability(
    "firebolt",
    Target.ENEMY_UNIT,
    3,
    5,
    4,
    0,
) {

    private val damage: Int get() =  30 + 5 * level

    override fun activate(battle: Battle, unitCaster: UnitActor, tileX: Int, tileY: Int): Boolean {
        val actors = getTargets(battle.field, unitCaster, tileX, tileY)

        fun damageTargets() {
            for (actor in actors) {
                val unit = if (actor is UnitActor) actor else continue

                if (!battle.friendlyFire && actor.isAlly(unitCaster)) continue

                val fireVulnerability = unit.get(TraitComponent::class.java)?.fireVulnerability ?: 1f

                var finalDamage: Int = (damage * fireVulnerability).toInt()
                finalDamage -= unit.defense

                if (fireVulnerability <= 0f)
                    battle.showFloatingLabel("Immune!", Color.YELLOW, unit.centerX, unit.centerY)
                else
                    unit.dealDamage(finalDamage, unitCaster, battle)
            }
        }

        //resolve damage immediately so the state does not depend on the animation
        damageTargets()

        //effect
        val effect = ctmn.petals.effects.FireballEffect(unitCaster.centerX, unitCaster.centerY, tileX * Config.TILE_SIZE + Config.TILE_SIZE / 2, tileY * Config.TILE_SIZE + Config.TILE_SIZE / 2)
        battle.queueSeqAction(effect)
        battle.queueSeqAction(OneSeqAction { effect.remove() })

        castTime = 0.20f

        return true
    }
}
