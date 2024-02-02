package ctmn.petals.unit.abilities

import ctmn.petals.GameConst
import ctmn.petals.playscreen.PlayScreen
import ctmn.petals.tile.TileActor
import ctmn.petals.unit.Ability
import ctmn.petals.unit.UnitActor
import ctmn.petals.utils.setPosByCenter
import ctmn.petals.utils.unTiled

class EarthcrackAbility : Ability(
    "earthcrack",
    Target.TILE,
    7,
    5,
    5,
    0,
    Type.OTHER,
) {

    init {
        skipConfirmation = true
        defGUI = false

        castAmounts = 3
        castAmountsLeft = castAmounts
    }

    override fun activate(playScreen: PlayScreen, unitCaster: UnitActor, tileX: Int, tileY: Int): Boolean {
        val playStage = playScreen.playStage

        //add tile to stage
        val oldTile = playStage.getTile(tileX, tileY) ?: return false
        val replaceTile =
            when (oldTile.terrain) {
                "forest" -> TileActor("fallen_forest", "fallenforest")
                "grass" -> TileActor("earthcrack", "earthcrack")
                "fallenforest" -> TileActor("earthcrack", "earthcrack")
                "earthcrack" -> null
                else -> return false
            }

        replaceTile?.let {
            oldTile.remove()
            it.setPosition(tileX, tileY)
            playStage.addActor(it)
        }

        //create effect
        val effect = ctmn.petals.effects.CreateEffect.earthcrack
        effect.setPosByCenter(
            tileX.unTiled() + GameConst.TILE_SIZE / 2,
            tileY.unTiled() + GameConst.TILE_SIZE / 2
        )
        playStage.addActor(effect)

        castTime = effect.lifeTime

        return true
    }
}