package ctmn.petals.tile.components

import com.badlogic.ashley.core.Component
import ctmn.petals.Const

data class CrystalsComponent(var amount: Int = Const.CRYSTALS_CLUSTER) : Component