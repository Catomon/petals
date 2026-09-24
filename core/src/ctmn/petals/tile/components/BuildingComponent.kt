package ctmn.petals.tile.components

import ctmn.petals.utils.serialization.SyncedComponent

import com.badlogic.ashley.core.Component

data class BuildingComponent(var buildingName: String, var playerId: Int, var turns: Int) : SyncedComponent