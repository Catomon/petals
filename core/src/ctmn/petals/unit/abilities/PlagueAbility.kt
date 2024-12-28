package ctmn.petals.unit.abilities

import ctmn.petals.Const
import ctmn.petals.playscreen.PlayScreen
import ctmn.petals.playscreen.playStage
import ctmn.petals.tile.TileActor
import ctmn.petals.tile.actors.newBurningForestTile
import ctmn.petals.unit.*
import ctmn.petals.unit.component.BurningComponent
import ctmn.petals.utils.setPosByCenter
import ctmn.petals.utils.unTiled

class PlagueAbility : Ability(
    "plague",
    Target.ALL, //Target.ENEMY_UNIT
    2,
    5,
    3,
    1,
) {

    private val damage: Int get() =  Damage.BURN + 5 * level

    override fun activate(playScreen: PlayScreen, unitCaster: UnitActor, tileX: Int, tileY: Int): Boolean {
        val actors = getTargets(playScreen.playStage, unitCaster, tileX, tileY)

        for (actor in actors) {
            val unit = if (actor is UnitActor) actor else continue //actor as UnitActor

            if (!playScreen.friendlyFire && actor.isAlly(unitCaster)) continue

            unit.dealDamage(damage, unitCaster, playScreen)
            unit.add(BurningComponent(unitCaster.playerId))
        }

        val playStage = actors.first().playStage

//        for (actor in actors) {
//            if (actor is TileActor && actor.tileName.startsWith("tree")) {
//                actor.remove()
//                playStage.addActor(
//                    newBurningForestTile
//                    .apply { tileComponent.layer = actor.layer; setPosition(actor.tiledX, actor.tiledY) })
//            }
//        }

        val effect =
            ctmn.petals.effects.FlameAbilityEffect(activationRange, tileX, tileY, playScreen.assets)
        effect.setPosByCenter(
            tileX.unTiled() + Const.TILE_SIZE / 2,
            tileY.unTiled() + Const.TILE_SIZE / 2)
        playScreen.playStage.addActor(effect)

        castTime = effect.lifeTime

        return true
    }
}
