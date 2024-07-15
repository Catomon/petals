package ctmn.petals.tile.components

import com.badlogic.ashley.core.Component

data class BaseBuildingComponent(var playerId: Int, var turns: Int) : Component