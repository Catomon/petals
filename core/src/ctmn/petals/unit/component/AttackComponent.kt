package ctmn.petals.unit.component
import com.badlogic.ashley.core.Component
import ctmn.petals.utils.CopyableComponent

data class AttackComponent(
    var minDamage: Int = 0,
    var maxDamage: Int = 0,
    var attackRange: Int = 0, //attack range end
    var attackRangeBlocked: Int = 0 //attack range start
) : Component, CopyableComponent {

    override fun makeCopy(): Component {
        return copy()
    }
}