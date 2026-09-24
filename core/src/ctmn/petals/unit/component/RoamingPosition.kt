package ctmn.petals.unit.component

import ctmn.petals.utils.serialization.SyncedComponent

import com.badlogic.ashley.core.Component

class RoamingPosition(val tiledX: Int, val tiledY: Int) : SyncedComponent