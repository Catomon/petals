package ctmn.petals.unit.component

import ctmn.petals.utils.serialization.SyncedComponent

import com.badlogic.ashley.core.Component
import ctmn.petals.utils.CopyableComponent

class InvisibilityComponent() : SyncedComponent, CopyableComponent {
    override fun makeCopy(): Component {
        return InvisibilityComponent()
    }
}