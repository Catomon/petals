package ctmn.petals.unit.abilities

import ctmn.petals.Config
import ctmn.petals.hasAssets
import ctmn.petals.sim.Battle
import ctmn.petals.tile.TerrainNames
import ctmn.petals.tile.TileActor
import ctmn.petals.unit.Ability
import ctmn.petals.unit.UnitActor
import ctmn.petals.utils.setPosByCenter

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

    override fun activate(battle: Battle, unitCaster: UnitActor, tileX: Int, tileY: Int): Boolean {
        //add tile to field
        val oldTile = battle.field.getTile(tileX, tileY) ?: return false
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
            battle.field.removeGameActor(oldTile)
            it.setPosition(tileX, tileY)
            battle.field.addGameActor(it)
        }

        // Visual only, and it reads the atlas, so a headless battle skips it.
        if (hasAssets) {
            val effect = ctmn.petals.effects.CreateEffect.earthcrack
            effect.setPosByCenter(
                tileX * Config.TILE_SIZE + Config.TILE_SIZE / 2,
                tileY * Config.TILE_SIZE + Config.TILE_SIZE / 2
            )
            battle.showEffect(effect)

            castTime = effect.lifeTime
        }

        return true
    }
}
