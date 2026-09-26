package ctmn.petals.unit.abilities

import ctmn.petals.sim.Battle
import ctmn.petals.unit.Ability
import ctmn.petals.unit.UnitActor
import ctmn.petals.utils.*

class TeleportAbility : Ability(
    "teleport",
    Target.TILE_PASSABLE,
    10,
    5,
    8,
    0,
) {

    override fun activate(battle: Battle, unitCaster: UnitActor, tileX: Int, tileY: Int): Boolean {
        val destinationTile = getTargets(battle.field, unitCaster, tileX, tileY).firstOrNull() ?: return false

        val startTileEffect = ctmn.petals.effects.CreateEffect.teleport
        startTileEffect.setPosByCenter(unitCaster.centerX, unitCaster.centerY)
        battle.queueSeqAction(startTileEffect)
        val endTileEffect = ctmn.petals.effects.CreateEffect.teleport
        endTileEffect.setPosByCenter(destinationTile.tileCenterX, destinationTile.tileCenterY)
        battle.queueSeqAction(endTileEffect)

        castTime = endTileEffect.lifeTime

        unitCaster.setPosition(tileX, tileY)

        return true
    }
}
