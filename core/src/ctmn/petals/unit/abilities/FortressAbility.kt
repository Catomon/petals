package ctmn.petals.unit.abilities

import ctmn.petals.GameConst
import ctmn.petals.playscreen.PlayScreen
import ctmn.petals.tile.Terrain
import ctmn.petals.tile.TileActor
import ctmn.petals.unit.Ability
import ctmn.petals.unit.UnitActor
import ctmn.petals.utils.setPosByCenter
import ctmn.petals.utils.unTiled

class FortressAbility : Ability(
    "fortress",
    Target.TILE,
    12,
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
                Terrain.forest -> TileActor("plant_tower", Terrain.tower)
                Terrain.grass -> TileActor("plant_wall", Terrain.walls)
                Terrain.fallenforest -> TileActor("plant_wall", Terrain.walls)
                Terrain.mountains -> TileActor("plant_fortress", Terrain.fortress)
                Terrain.earthcrack -> null
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