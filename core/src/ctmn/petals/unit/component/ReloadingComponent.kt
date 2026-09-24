package ctmn.petals.unit.component

import ctmn.petals.utils.serialization.SyncedComponent

import com.badlogic.ashley.core.Component
import ctmn.petals.utils.CopyableComponent

data class ReloadingComponent(val turns: Int, val maxAmmo: Int = 1, var currentTurns: Int = 0) : SyncedComponent, CopyableComponent {
    override fun makeCopy(): Component {
        return copy()
    }
}