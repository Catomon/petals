package ctmn.petals.tile.components

import com.badlogic.ashley.core.Component

data class CapturingComponent(var playerId: Int, var turns: Int) : Component