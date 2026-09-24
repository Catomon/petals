package ctmn.petals.unit.component

import ctmn.petals.utils.serialization.SyncedComponent

import com.badlogic.ashley.core.Component
import ctmn.petals.unit.buff.Buff
import ctmn.petals.utils.CopyableComponent

data class BuffsComponent(var buffs: ArrayList<Buff> = ArrayList()) : SyncedComponent, CopyableComponent {
    override fun makeCopy(): Component {
        return BuffsComponent(ArrayList<Buff>().apply { buffs.forEach { add(it.copy()) } })
    }
}