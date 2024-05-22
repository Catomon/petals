package ctmn.petals.unit.component
import com.badlogic.ashley.core.Component
import ctmn.petals.utils.CopyableComponent

const val ATTACK_TYPE_GROUND = "ground"
const val ATTACK_TYPE_AIR = "air"
//const val ATTACK_TYPE_WATER = "water"
const val ATTACK_TYPE_ALL = "ground & air" // & water
//const val ATTACK_TYPE_GROUND_AND_AIR = "ground & air"

data class AttackComponent(
    var minDamage: Int = 0,
    var maxDamage: Int = 0,
    var attackRange: Int = 0, //attack range end
    var attackRangeBlocked: Int = 0, //attack range start
    var attackType: String = ATTACK_TYPE_GROUND
) : Component, CopyableComponent {

    override fun makeCopy(): Component {
        return copy()
    }
}