package ctmn.petals.unit.abilities

import ctmn.petals.playscreen.PlayScreen
import ctmn.petals.playscreen.addAction
import ctmn.petals.playscreen.addOnCompleteTrigger
import ctmn.petals.playscreen.seqactions.ActorAction
import ctmn.petals.playscreen.seqactions.ThrowUnitAction
import ctmn.petals.unit.UnitActor
import ctmn.petals.unit.dealDamage
import ctmn.petals.actors.actions.JumpAction
import ctmn.petals.actors.actions.OneAction
import ctmn.petals.utils.unTiled
import com.badlogic.gdx.scenes.scene2d.actions.Actions
import ctmn.petals.effects.CreateEffect
import ctmn.petals.unit.Ability

open class JumpAbility : Ability("jump") {

    var damage: Int = 0
    var pushUnits: Boolean = false

    init {
        target = Target.TILE_PASSABLE_INCLUDE_SELF
        range = 3
        castTime = 2f
    }

    override fun activate(playScreen: PlayScreen, unitCaster: UnitActor, tileX: Int, tileY: Int): Boolean {
        playScreen.addAction(ActorAction(unitCaster,
            Actions.sequence(
                JumpAction(unitCaster.x, unitCaster.y, tileX.unTiled(), tileY.unTiled()),
                OneAction {
                    if (!pushUnits)
                        return@OneAction

                    fun UnitActor.throwUnit(tileX: Int, tileY: Int) {
                        playScreen.addAction(ThrowUnitAction(this, tileX, tileY)).addOnCompleteTrigger { _ ->
                            CreateEffect.damageUnit(
                                this.dealDamage(damage, unitCaster, playScreen)
                            )
                        }
                    }

                    playScreen.playStage.getUnit(tileX, tileY + 1)?.throwUnit(0, 1)
                    playScreen.playStage.getUnit(tileX, tileY - 1)?.throwUnit(0, -1)
                    playScreen.playStage.getUnit(tileX - 1, tileY)?.throwUnit(-1, 0)
                    playScreen.playStage.getUnit(tileX + 1, tileY)?.throwUnit(1, 0)
                }
            )))

        return true
    }
}