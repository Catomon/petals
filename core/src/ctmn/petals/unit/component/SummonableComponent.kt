package ctmn.petals.unit.component

import com.badlogic.ashley.core.Component
import ctmn.petals.utils.CopyableComponent

data class SummonableComponent(var cost: Int = 25) : Component, CopyableComponent {
    override fun makeCopy(): Component {
        return copy()
    }
}