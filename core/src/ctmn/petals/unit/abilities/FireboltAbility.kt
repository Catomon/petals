package ctmn.petals.unit.abilities

import ctmn.petals.Const
import ctmn.petals.effects.CreateEffect
import ctmn.petals.effects.FireballEffect
import ctmn.petals.playscreen.PlayScreen
import ctmn.petals.playscreen.queueAction
import ctmn.petals.playscreen.seqactions.UpdateAction
import ctmn.petals.unit.*
import ctmn.petals.unit.UnitActor
import ctmn.petals.unit.component.TraitComponent
import ctmn.petals.utils.centerX
import ctmn.petals.utils.centerY
import ctmn.petals.utils.unTiled

class FireboltAbility : Ability(
    "firebolt",
    Target.ENEMY_UNIT,
    3,
    5,
    4,
    0,
) {

    private val damage: Int get() =  30 + 5 * level

    override fun activate(playScreen: PlayScreen, unitCaster: UnitActor, tileX: Int, tileY: Int): Boolean {
        val playStage = playScreen.playStage

        val actors = getTargets(playScreen.playStage, unitCaster, tileX, tileY)

        fun damageTargets() {
            for (actor in actors) {
                val unit = if (actor is UnitActor) actor else continue

                if (!playScreen.friendlyFire && actor.isAlly(unitCaster)) continue

                val fireVulnerability = unit.get(TraitComponent::class.java)?.fireVulnerability ?: 1f

                var finalDamage: Int = (damage * fireVulnerability).toInt()
                finalDamage -= unit.defense

                if (fireVulnerability <= 0f)
                    CreateEffect.immuneEffect(unit)
                else
                    unit.dealDamage(finalDamage, unitCaster, playScreen)
            }
        }

        //effect
        val effect = FireballEffect(unitCaster.centerX, unitCaster.centerY, tileX.unTiled() + Const.TILE_SIZE / 2, tileY.unTiled() + Const.TILE_SIZE / 2)
        playStage.addActor(effect)

        playScreen.queueAction(UpdateAction {
            if (effect.isDone) {
                damageTargets()

                true
            } else
                false
        })

        castTime = 0.20f

        return true
    }
}
