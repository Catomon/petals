package ctmn.petals.unit.abilities

import ctmn.petals.Config
import ctmn.petals.effects.CreateEffect
import ctmn.petals.sim.Battle
import ctmn.petals.tile.TileActor
import ctmn.petals.unit.Ability
import ctmn.petals.unit.UnitActor
import ctmn.petals.unit.throwAction
import ctmn.petals.utils.setPosByCenter

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

    override fun activate(battle: Battle, unitCaster: UnitActor, tileX: Int, tileY: Int): Boolean {
        //pick replace tiles
        val oldTile = battle.field.getTile(tileX, tileY) ?: return false
        val replaceTile =
            when (oldTile.terrain) {
                "forest" -> TileActor("crater", "earthcrack")
                "grass" -> TileActor("crater", "earthcrack")
                "fallenforest" -> TileActor("crater", "earthcrack")
                "earthcrack" -> null
                else -> null
            }

        //replace the tile and throw the surrounding units immediately, then only queue the visuals
        replaceTile?.let {
            battle.field.removeGameActor(oldTile)
            it.setPosition(tileX, tileY)
            battle.field.addGameActor(it)
        }

        battle.field.getUnit(tileX, tileY)?.throwAction(unitCaster, 0, 0, damage, battle)
        battle.field.getUnit(tileX, tileY + 1)?.throwAction(unitCaster, 0, 1, damageSides, battle)
        battle.field.getUnit(tileX, tileY - 1)?.throwAction(unitCaster, 0, -1, damageSides, battle)
        battle.field.getUnit(tileX - 1, tileY)?.throwAction(unitCaster, -1, 0, damageSides, battle)
        battle.field.getUnit(tileX + 1, tileY)?.throwAction(unitCaster, 1, 0, damageSides, battle)

        //create effect
        val effect = CreateEffect.meteorite
        effect.setPosByCenter(
            tileX * Config.TILE_SIZE + Config.TILE_SIZE / 2,
            tileY * Config.TILE_SIZE + Config.TILE_SIZE / 2
        )
        battle.queueSeqAction(effect)

        //set cast time
        castTime = 0.25f //effect.lifeTime + 1.25f

        return true
    }
}
