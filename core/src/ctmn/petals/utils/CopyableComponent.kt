package ctmn.petals.utils

import com.badlogic.ashley.core.Component

interface CopyableComponent {
    fun makeCopy(): Component
}