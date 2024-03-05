package ctmn.petals.unit.abilities

import ctmn.petals.Const
import ctmn.petals.playscreen.PlayScreen
import ctmn.petals.tile.TerrainNames
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
                TerrainNames.forest -> TileActor("plant_tower", TerrainNames.tower)
                TerrainNames.grass -> TileActor("plant_wall", TerrainNames.walls)
                TerrainNames.fallenforest -> TileActor("plant_wall", TerrainNames.walls)
                TerrainNames.mountains -> TileActor("plant_fortress", TerrainNames.fortress)
                TerrainNames.earthcrack -> null
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
            tileX.unTiled() + Const.TILE_SIZE / 2,
            tileY.unTiled() + Const.TILE_SIZE / 2
        )
        playStage.addActor(effect)

        castTime = effect.lifeTime

        return true
    }
}