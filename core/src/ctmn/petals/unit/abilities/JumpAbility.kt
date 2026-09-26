package ctmn.petals.unit.abilities

import ctmn.petals.Config
import ctmn.petals.actors.actions.CameraShakeAction
import ctmn.petals.actors.actions.JumpAction
import ctmn.petals.actors.actions.OneAction
import ctmn.petals.playscreen.seqactions.ActorAction
import ctmn.petals.playscreen.seqactions.ThrowUnitAction
import ctmn.petals.playscreen.playStageOrNull
import ctmn.petals.sim.Battle
import ctmn.petals.unit.Ability
import ctmn.petals.unit.UnitActor
import ctmn.petals.unit.dealDamage
import ctmn.petals.unit.isAlly
import ctmn.petals.utils.tiledX
import ctmn.petals.utils.tiledY
import com.badlogic.gdx.scenes.scene2d.actions.Actions

open class JumpAbility : Ability("jump") {

    var damage: Int = 0
    var pushUnits: Boolean = false

    init {
        target = Target.TILE_PASSABLE_INCLUDE_SELF
        range = 3
        castTime = 2f
    }

    override fun activate(battle: Battle, unitCaster: UnitActor, tileX: Int, tileY: Int): Boolean {
        //resolve the jump and any pushed units immediately, then only queue the visuals
        unitCaster.setPosition(tileX, tileY)

        if (pushUnits) {
            fun UnitActor.throwUnit(throwX: Int, throwY: Int) {
                if (this.isAlly(unitCaster) && !battle.friendlyFire)
                    return

                this.dealDamage(damage, unitCaster, battle)
                battle.queueSeqAction(ThrowUnitAction(this, throwX, throwY))
            }

            battle.field.getUnit(tileX, tileY + 1)?.throwUnit(0, 1)
            battle.field.getUnit(tileX, tileY - 1)?.throwUnit(0, -1)
            battle.field.getUnit(tileX - 1, tileY)?.throwUnit(-1, 0)
            battle.field.getUnit(tileX + 1, tileY)?.throwUnit(1, 0)
        }

        battle.queueSeqAction(ActorAction(unitCaster,
            Actions.sequence(
                JumpAction(unitCaster.x, unitCaster.y, tileX * Config.TILE_SIZE, tileY * Config.TILE_SIZE),
                OneAction {
                    if (!pushUnits)
                        return@OneAction

                    unitCaster.playStageOrNull?.addAction(CameraShakeAction(2f))
                }
            )))

        return true
    }
}
