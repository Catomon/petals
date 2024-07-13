package ctmn.petals.unit.component

import com.badlogic.ashley.core.Component
import ctmn.petals.utils.CopyableComponent

data class ReloadingComponent(val turns: Int, val maxAmmo: Int = 1, var currentTurns: Int = 0) : Component, CopyableComponent {
    override fun makeCopy(): Component {
        return copy()
    }
}