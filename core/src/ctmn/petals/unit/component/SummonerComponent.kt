package ctmn.petals.unit.component

import com.badlogic.ashley.core.Component
import ctmn.petals.utils.CopyableComponent

data class SummonerComponent(
    // unitId:cost
    val units: MutableSet<String> = mutableSetOf(),
    var selectedUnit: String? = null,

    var giveAP: Boolean = false,
    var maxUnits: Int = 4,
) : Component, CopyableComponent {

    override fun makeCopy(): Component {
        return SummonerComponent(mutableSetOf<String>().apply { addAll(this@SummonerComponent.units) }, selectedUnit, giveAP, maxUnits)
    }
}