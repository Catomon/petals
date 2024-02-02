package ctmn.petals.unit.abilities

import ctmn.petals.GameConst
import ctmn.petals.effects.CreateEffect
import ctmn.petals.playscreen.PlayScreen
import ctmn.petals.unit.Ability
import ctmn.petals.unit.UnitActor
import ctmn.petals.utils.*

class UnsummonAbility : Ability(
    "unsummon",
    Target.MY_UNIT,
    1,
    5,
    0,
    0,
) {

    override fun activate(playScreen: PlayScreen, unitCaster: UnitActor, tileX: Int, tileY: Int): Boolean {
        val playStage = playScreen.playStage

        unitCaster.remove()

        playScreen.guiStage.selectUnit(null)
        playScreen.guiStage.abilitiesPanel.setUpForUnit(null, false)

        //create effect
        val effect = CreateEffect.summon
        effect.setPosition(
            tileX.unTiled() + GameConst.TILE_SIZE / 2,
            tileY.unTiled() + GameConst.TILE_SIZE / 2
        )
        playStage.addActor(effect)

        castTime = effect.lifeTime * 0.70f

        return true
    }
}
