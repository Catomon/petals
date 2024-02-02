package ctmn.petals.unit.component

import com.badlogic.ashley.core.Component
import ctmn.petals.utils.CopyableComponent

class InvisibilityComponent() : Component, CopyableComponent {
    override fun makeCopy(): Component {
        return InvisibilityComponent()
    }
}