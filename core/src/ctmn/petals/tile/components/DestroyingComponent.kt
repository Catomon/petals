package ctmn.petals.tile.components

import ctmn.petals.utils.serialization.SyncedComponent

import com.badlogic.ashley.core.Component

data class DestroyingComponent(var playerId: Int) : SyncedComponent