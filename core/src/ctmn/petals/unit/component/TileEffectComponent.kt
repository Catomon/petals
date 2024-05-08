package ctmn.petals.unit.component

import com.badlogic.ashley.core.Component
import ctmn.petals.utils.CopyableComponent

/** Being added to units that stood on a tile with effect, like lava time dealing damage,
 * to mark that they got the damage.
 * @param [playerId] - player on whose turn the unit stood on a tile */
data class TileEffectComponent(var playerId: Int) : Component, CopyableComponent {
    override fun makeCopy(): Component {
        return copy()
    }
}