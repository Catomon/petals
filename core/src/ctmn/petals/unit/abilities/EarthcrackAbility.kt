package ctmn.petals.unit.abilities

import ctmn.petals.Config
import ctmn.petals.hasAssets
import ctmn.petals.sim.Battle
import ctmn.petals.tile.TileActor
import ctmn.petals.unit.Ability
import ctmn.petals.unit.UnitActor
import ctmn.petals.utils.setPosByCenter

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

    override fun activate(battle: Battle, unitCaster: UnitActor, tileX: Int, tileY: Int): Boolean {
        val oldTile = battle.field.getTile(tileX, tileY) ?: return false
        val replaceTile =
            when (oldTile.terrain) {
                "forest" -> TileActor("fallen_forest", "fallenforest")
                "grass" -> TileActor("earthcrack", "earthcrack")
                "fallenforest" -> TileActor("earthcrack", "earthcrack")
                "earthcrack" -> null
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
