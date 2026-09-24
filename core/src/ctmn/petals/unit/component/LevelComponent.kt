package ctmn.petals.unit.component

import ctmn.petals.utils.serialization.SyncedComponent

import com.badlogic.ashley.core.Component
import ctmn.petals.utils.CopyableComponent

data class LevelComponent(
    var lvl: Int = 1,
    var hpPerLvl: Int = 0,
    var dfPerLvl: Int = 1,
    var dmgPerLvl: Int = 2,
    var apPerLvl: Int = 2,
    var mpPerLvl: Int = 2,
    var exp: Int = 0,
    var hp: Int = 0,
    var df: Int = 0,
    var dmg: Int = 0,
    var mp: Int = 0,
) : SyncedComponent, CopyableComponent {

    override fun makeCopy(): Component {
        return copy()
    }
}