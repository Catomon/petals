package ctmn.petals.tile.components

import com.badlogic.ashley.core.Component

data class BuildingComponent(var buildingName: String, var playerId: Int, var turns: Int) : Component