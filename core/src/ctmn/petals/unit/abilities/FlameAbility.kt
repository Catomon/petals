package ctmn.petals.unit.abilities

import ctmn.petals.Config
import ctmn.petals.sim.Battle
import ctmn.petals.utils.setPosByCenter
import ctmn.petals.tile.TileActor
import ctmn.petals.tile.actors.newBurningForestTile
import ctmn.petals.unit.*
import ctmn.petals.unit.component.BurningComponent

class FlameAbility : Ability(
    "flame",
    Target.ALL, //Target.ENEMY_UNIT
    2,
    5,
    3,
    1,
) {

    private val damage: Int get() =  Damage.BURN + 5 * level

    override fun activate(battle: Battle, unitCaster: UnitActor, tileX: Int, tileY: Int): Boolean {
        val actors = getTargets(battle.field, unitCaster, tileX, tileY)

        for (actor in actors) {
            val unit = if (actor is UnitActor) actor else continue //actor as UnitActor

            if (!battle.friendlyFire && actor.isAlly(unitCaster)) continue

            unit.dealDamage(damage, unitCaster, battle)
            unit.add(BurningComponent(unitCaster.playerId))
        }

        for (actor in actors) {
            if (actor is TileActor && actor.tileName.startsWith("tree")) {
                battle.field.removeGameActor(actor)
                battle.field.addGameActor(
                    newBurningForestTile
                    .apply { tileComponent.layer = actor.layer; setPosition(actor.tiledX, actor.tiledY) })
            }
        }

        val effect =
            ctmn.petals.effects.FlameAbilityEffect(activationRange, tileX, tileY, ctmn.petals.assets)
        effect.setPosByCenter(
            tileX * Config.TILE_SIZE + Config.TILE_SIZE / 2,
            tileY * Config.TILE_SIZE + Config.TILE_SIZE / 2)
        battle.queueSeqAction(effect)

        castTime = effect.lifeTime

        return true
    }
}
