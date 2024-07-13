package ctmn.petals.unit.component

import com.badlogic.ashley.core.Component
import ctmn.petals.utils.CopyableComponent

data class MoveAfterAttackComponent(val normalRange: Int, val secondRange: Int, var attacked: Boolean = false) : Component, CopyableComponent {
    override fun makeCopy(): Component {
        return copy()
    }
}