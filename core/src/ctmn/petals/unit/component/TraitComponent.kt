package ctmn.petals.unit.component

import ctmn.petals.utils.serialization.SyncedComponent

import com.badlogic.ashley.core.Component
import ctmn.petals.utils.CopyableComponent

// fireVulnerability 0f means immune to fire, 1f - normal
data class TraitComponent(var fireVulnerability: Float = 1f) : SyncedComponent, CopyableComponent {

    override fun makeCopy(): Component {
        return copy()
    }
}