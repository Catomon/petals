package ctmn.petals.unit.abilities

import ctmn.petals.playscreen.PlayScreen
import ctmn.petals.unit.Ability
import ctmn.petals.unit.UnitActor
import ctmn.petals.unit.component.InvisibilityComponent
import ctmn.petals.utils.centerX
import ctmn.petals.utils.centerY
import ctmn.petals.utils.setPosByCenter

class InvisibilityAbility : Ability(
    "invisibility",
    Target.MY_TEAM_UNIT,
    10,
    5,
    3,
    0,
) {

    override fun activate(playScreen: PlayScreen, unitCaster: UnitActor, tileX: Int, tileY: Int): Boolean {
        val actors = getTargets(playScreen.playStage, unitCaster, tileX, tileY)

        for (actor in actors) {
            val unit = actor as UnitActor

            unit.add(InvisibilityComponent())

            val effect = ctmn.petals.effects.CreateEffect.invisibility
            effect.setPosByCenter(actor.centerX, actor.centerY)
            playScreen.playStage.addActor(effect)
            castTime = effect.lifeTime
        }

        return true
    }
}
