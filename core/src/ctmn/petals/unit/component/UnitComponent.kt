package ctmn.petals.unit.component

import ctmn.petals.Const
import ctmn.petals.player.Player
import ctmn.petals.player.Team
import com.badlogic.ashley.core.Component
import ctmn.petals.utils.CopyableComponent

const val UNIT_TYPE_LAND = "land"
const val UNIT_TYPE_WATER = "water"
const val UNIT_TYPE_AIR = "air"

data class UnitComponent(
    var name: String,
    var health: Int,
    var defense: Int,
    var movingRange: Int,
    var viewRange: Int,
    var type: String = UNIT_TYPE_LAND,
    var playerID: Int = Player.NONE,
    var teamID: Int = Team.NONE,
    var tiledX: Int = 0,
    var tiledY: Int = 0,
    var actionPoints: Int = Const.ACTION_POINTS,
    var allies: MutableSet<Int> = mutableSetOf(),
) : Component, CopyableComponent {

    val baseHealth = health

    override fun makeCopy(): Component {
        return copy().also { it.allies = mutableSetOf<Int>().apply { addAll(this@UnitComponent.allies) } }
    }
}