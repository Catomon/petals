package ctmn.petals.unit.abilities

import ctmn.petals.Config
import ctmn.petals.sim.Battle
import ctmn.petals.tile.TerrainNames
import ctmn.petals.tile.TileActor
import ctmn.petals.tile.Tiles
import ctmn.petals.tile.components.LifeTimeComponent
import ctmn.petals.unit.Ability
import ctmn.petals.unit.UnitActor
import ctmn.petals.utils.setPosByCenter

class FogAbility : Ability(
    "fog",
    Target.ALL,
    8,
    5,
    3,
    2,
) {

    private val lifeTime get() = 1f + 1 * level

    override fun activate(battle: Battle, unitCaster: UnitActor, tileX: Int, tileY: Int): Boolean {
        val actors = getTargets(battle.field, unitCaster, tileX, tileY)

        for (actor in actors) {
            if (actor is TileActor && actor.terrain != TerrainNames.impassable) {
                battle.field.addGameActor(
                    TileActor(Tiles.FOG, TerrainNames.fog).apply {
                        tileComponent.layer = 10; setPosition(actor.tiledX, actor.tiledY)
                        add(LifeTimeComponent(lifeTime))
                    })
            }
        }

        val effect = ctmn.petals.effects.CreateEffect.fog
        effect.setPosByCenter(
            tileX * Config.TILE_SIZE + Config.TILE_SIZE / 2,
            tileY * Config.TILE_SIZE + Config.TILE_SIZE / 2)
        battle.queueSeqAction(effect)

        castTime = effect.lifeTime

        return true
    }
}
