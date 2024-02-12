package ctmn.petals.unit

import ctmn.petals.tile.TileActor
import ctmn.petals.tile.isPassableAndFree
import ctmn.petals.utils.tiled
import com.badlogic.gdx.scenes.scene2d.Actor
import com.badlogic.gdx.utils.Array
import ctmn.petals.playscreen.*
import ctmn.petals.playstage.PlayStage
import ctmn.petals.playstage.getTilesAndUnits
import ctmn.petals.playstage.tiledDst

/** Classes that inherit this class should not have important mutable fields
 * because only base class fields are getting serialized.
 * Proper field: val damage: Int get() =  30 + 10 * level */
abstract class Ability(
    val name: String,
    var target: Target = Target.ALL,
    var cooldown: Int = 5,
    var cost: Int = 1,
    var range: Int = 3,            //if <=0, activates on unit that uses it
    var activationRange: Int = 0,  //if 0, activates on single selected tile
    var type: Type = Type.OTHER,
    var level: Int = 0,
    var castAmounts: Int = 1,
) {

    //val name: String = name
        //get() { return if (level > 1) field + "_" + level else field }

    var castAmountsLeft = castAmounts
    var currentCooldown: Int  = 0

    var skipConfirmation: Boolean = false
    var defGUI: Boolean = true
    var castTime: Float = 1f

    /** Should be reusable
     * @return true on success */
    abstract fun activate(playScreen: PlayScreen, unitCaster: UnitActor, tileX: Int, tileY: Int): Boolean

    enum class Target {
        ALL,
        UNIT,
        TILE,
        TILE_PASSABLE,
        TILE_PASSABLE_INCLUDE_SELF,
        MY_UNIT,
        ENEMY_UNIT,
        MY_TEAM_UNIT,
        ALLY_UNIT,
        ALL_MY_LEADER_FOLLOWERS,
        ALL_MY_TEAM_LEADER_FOLLOWERS,
        ALL_TEAMMATE_LEADER_FOLLOWERS,
        ALL_ENEMY_LEADER_FOLLOWERS,
        UNIT_FOLLOWER,
        UNIT_MONSTER,
        OBJECTIVE
    }

    fun getTargets(playStage: PlayStage, unitCaster: UnitActor, castX: Int, castY: Int) : Array<Actor> {
        val targetActorsArray = Array<Actor>()

        for (actor in playStage.getTilesAndUnits()) {
            if (actor is TileActor || actor is UnitActor) {
                val actorX = if (actor is TileActor) actor.tiledX else if (actor is UnitActor) actor.tiledX else actor.x.tiled()
                val actorY = if (actor is TileActor) actor.tiledY else if (actor is UnitActor) actor.tiledY else actor.y.tiled()
                if (tiledDst(castX, castY, actorX, actorY) <= activationRange)
                    when (target) {
                        Target.ALL -> targetActorsArray.add(actor)
                        Target.UNIT -> if (actor is UnitActor) targetActorsArray.add(actor)
                        Target.TILE -> if (actor is TileActor) targetActorsArray.add(actor)
                        Target.TILE_PASSABLE -> if (actor is TileActor && actor.isPassableAndFree()) targetActorsArray.add(actor)
                        Target.TILE_PASSABLE_INCLUDE_SELF -> if (actor is TileActor && (actor.isPassableAndFree() || playStage.getUnit(actorX, actorY) == unitCaster)) targetActorsArray.add(actor)
                        Target.MY_UNIT -> if (actor is UnitActor && actor.playerId == unitCaster.playerId) targetActorsArray.add(actor)
                        Target.ENEMY_UNIT -> if (actor is UnitActor && !unitCaster.isAlly(actor)) targetActorsArray.add(actor)
                        Target.MY_TEAM_UNIT -> if (actor is UnitActor && actor.teamId == unitCaster.teamId) targetActorsArray.add(actor)
                        Target.ALLY_UNIT -> if (actor is UnitActor && unitCaster.isAlly(actor)) targetActorsArray.add(actor)
                        Target.UNIT_FOLLOWER -> if (actor is UnitActor && actor.playerId == unitCaster.playerId && actor.leaderID == unitCaster.leaderID) targetActorsArray.add(actor)
                        Target.UNIT_MONSTER -> TODO()
                        Target.OBJECTIVE -> TODO()
                        Target.ALL_MY_LEADER_FOLLOWERS -> TODO()
                        Target.ALL_MY_TEAM_LEADER_FOLLOWERS -> TODO()
                        Target.ALL_TEAMMATE_LEADER_FOLLOWERS -> TODO()
                        Target.ALL_ENEMY_LEADER_FOLLOWERS -> TODO()
                    }
            }
        }

        return targetActorsArray
    }

    override fun toString(): String {
        return name
    }

    enum class Type {
        DAMAGE,
        HEALING,
        CONTROL,
        BUFF,
        DEBUFF,
        MOVEMENT,
        OTHER
    }

    override fun hashCode(): Int {
        return name.hashCode()
    }

    override fun equals(other: Any?): Boolean {
        if (other !is Ability) return false

        return name == other.name
    }
}
