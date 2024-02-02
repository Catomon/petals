package ctmn.petals.unit.component
import com.badlogic.ashley.core.Component
import ctmn.petals.utils.CopyableComponent

data class FollowerComponent(var leaderID: Int = 0, var dieWithLeader: Boolean = false) : Component, CopyableComponent {
    override fun makeCopy(): Component {
        return copy()
    }
}