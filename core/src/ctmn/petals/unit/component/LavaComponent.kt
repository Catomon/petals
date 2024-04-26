package ctmn.petals.unit.component

import com.badlogic.ashley.core.Component
import ctmn.petals.utils.CopyableComponent

/** Units standing in lava tile get these. [playerId] - player on which turn the unit got in lava*/
data class LavaComponent(var playerId: Int) : Component, CopyableComponent {
    override fun makeCopy(): Component {
        return copy()
    }
}