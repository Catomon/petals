package ctmn.petals.unit.component
import com.badlogic.ashley.core.Component
import ctmn.petals.utils.CopyableComponent

data class LeaderComponent(
    var leaderID: Int = 0,
    var leaderRange: Int = 0,
    var leaderDmgBuff: Int = 0,
    var leaderDefBuff: Int = 0,
    var maxUnits: Int  = 3,
    var killUnitsOnDeath: Boolean = false
    ) : Component, CopyableComponent {
    override fun makeCopy(): Component {
        return copy()
    }
}