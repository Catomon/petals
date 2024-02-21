package ctmn.petals.unit.abilities

import ctmn.petals.Const
import ctmn.petals.effects.CreateEffect
import ctmn.petals.playscreen.*
import ctmn.petals.playscreen.seqactions.UpdateAction
import ctmn.petals.tile.TileActor
import ctmn.petals.unit.Ability
import ctmn.petals.unit.UnitActor
import ctmn.petals.unit.throwAction
import ctmn.petals.utils.*

class MeteoriteAbility : Ability(
    "meteorite",
    Target.TILE,
    7,
    5,
    6,
    0,
) {

    private val damage get() = 25 + 5 * level
    private val damageSides get() = 10 + 5 * level

    override fun activate(playScreen: PlayScreen, unitCaster: UnitActor, tileX: Int, tileY: Int): Boolean {
        val playStage = playScreen.playStage

        //pick replace tiles
        val oldTile = playStage.getTile(tileX, tileY) ?: return false
        val replaceTile =
            when (oldTile.terrain) {
                "forest" -> TileActor("crater", "earthcrack")
                "grass" -> TileActor("crater", "earthcrack")
                "fallenforest" -> TileActor("crater", "earthcrack")
                "earthcrack" -> null
                else -> null
            }

        //create effect
        val effect = CreateEffect.meteorite
        effect.setPosByCenter(
            tileX.unTiled() + Const.TILE_SIZE / 2,
            tileY.unTiled() + Const.TILE_SIZE / 2
        )
        playStage.addActor(effect)

        //set cast time
        castTime = 0.25f //effect.lifeTime + 1.25f

        playScreen.queueAction(UpdateAction {
            if (effect.isDone) {
                //replace tile
                replaceTile?.let {
                    oldTile.remove()
                    it.setPosition(tileX, tileY)
                    playStage.addActor(it)
                }

                //throw units
                playScreen.playStage.getUnit(tileX, tileY)?.throwAction(unitCaster, 0, 0, damage, playScreen)
                playScreen.playStage.getUnit(tileX, tileY + 1)?.throwAction(unitCaster, 0, 1, damageSides, playScreen)
                playScreen.playStage.getUnit(tileX, tileY - 1)?.throwAction(unitCaster, 0, -1, damageSides, playScreen)
                playScreen.playStage.getUnit(tileX - 1, tileY)?.throwAction(unitCaster, -1, 0, damageSides, playScreen)
                playScreen.playStage.getUnit(tileX + 1, tileY)?.throwAction(unitCaster, 1, 0, damageSides, playScreen)
            }

            effect.isDone
        })

        return true
    }
}
