package ctmn.petals.unit.abilities

import ctmn.petals.playscreen.PlayScreen
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

    override fun activate(playScreen: PlayScreen, unitCaster: UnitActor, tileX: Int, tileY: Int): Boolean {
        val playStage = playScreen.playStage

        val destinationTile = getTargets(playScreen.playStage, unitCaster, tileX, tileY).firstOrNull() ?: return false

        val startTileEffect = ctmn.petals.effects.CreateEffect.teleport
        startTileEffect.setPosByCenter(unitCaster.centerX, unitCaster.centerY)
        playStage.addActor(startTileEffect)
        val endTileEffect = ctmn.petals.effects.CreateEffect.teleport
        endTileEffect.setPosByCenter(destinationTile.tileCenterX, destinationTile.tileCenterY)
        playStage.addActor(endTileEffect)

        castTime = endTileEffect.lifeTime

        unitCaster.setPosition(tileX, tileY)

        return true
    }
}
