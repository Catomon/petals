package ctmn.petals.unit.component

import com.badlogic.ashley.core.Component
import ctmn.petals.unit.Damage
import ctmn.petals.utils.CopyableComponent

/** Marks unit that are burning
 * @param [playerId] - player on whose turn the unit stood on a tile */
data class BurningComponent(var playerId: Int, var duration: Float = Damage.BURN_DURATION) : Component, CopyableComponent {
    override fun makeCopy(): Component {
        return copy()
    }
}