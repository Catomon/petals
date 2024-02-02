package ctmn.petals.unit.component

import ctmn.petals.GameConst
import ctmn.petals.player.Player
import ctmn.petals.player.Team
import com.badlogic.ashley.core.Component
import ctmn.petals.utils.CopyableComponent

data class UnitComponent(
    var name: String,
    var health: Int,
    var defense: Int,
    var movingRange: Int,
    var viewRange: Int,
    var playerID: Int = Player.NONE,
    var teamID: Int = Team.NONE,
    var tiledX: Int = 0,
    var tiledY: Int = 0,
    var actionPoints: Int = GameConst.ACTION_POINTS,
    var allies: MutableSet<Int> = mutableSetOf(),
) : Component, CopyableComponent {

    val baseHealth = health

    override fun makeCopy(): Component {
        return copy()
    }
}