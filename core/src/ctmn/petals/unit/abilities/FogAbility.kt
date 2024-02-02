package ctmn.petals.unit.abilities

import ctmn.petals.GameConst
import ctmn.petals.playscreen.PlayScreen
import ctmn.petals.tile.Terrain
import ctmn.petals.tile.TileActor
import ctmn.petals.tile.Tiles
import ctmn.petals.tile.components.LifeTimeComponent
import ctmn.petals.unit.Ability
import ctmn.petals.unit.UnitActor
import ctmn.petals.utils.setPosByCenter
import ctmn.petals.utils.unTiled

class FogAbility : Ability(
    "fog",
    Target.ALL,
    8,
    5,
    3,
    2,
) {

    private val lifeTime get() = 1f + 1 * level

    override fun activate(playScreen: PlayScreen, unitCaster: UnitActor, tileX: Int, tileY: Int): Boolean {
        val playStage = playScreen.playStage
        val actors = getTargets(playScreen.playStage, unitCaster, tileX, tileY)

        for (actor in actors) {
            if (actor is TileActor && actor.terrain != Terrain.impassable) {
                playStage.addActor(
                    TileActor(Tiles.FOG, Terrain.fog).apply {
                        tileComponent.layer = 10; setPosition(actor.tiledX, actor.tiledY)
                        add(LifeTimeComponent(lifeTime))
                    })
            }
        }

        val effect = ctmn.petals.effects.CreateEffect.fog
        effect.setPosByCenter(
            tileX.unTiled() + GameConst.TILE_SIZE / 2,
            tileY.unTiled() + GameConst.TILE_SIZE / 2)
        playStage.addActor(effect)

        castTime = effect.lifeTime

        return true
    }
}
