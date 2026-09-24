package ctmn.petals.tile.components

import ctmn.petals.utils.serialization.SyncedComponent

import com.badlogic.ashley.core.Component
import ctmn.petals.Config

data class CrystalsComponent(var amount: Int = Config.CRYSTALS_CLUSTER) : SyncedComponent