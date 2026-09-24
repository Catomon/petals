package ctmn.petals.unit.component

import ctmn.petals.utils.serialization.SyncedComponent

import com.badlogic.ashley.core.Component
import ctmn.petals.utils.CopyableComponent

data class BarrierComponent(var amount: Int) : SyncedComponent, CopyableComponent {
    override fun makeCopy(): Component {
        return copy()
    }
}