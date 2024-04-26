package ctmn.petals.unit.component

import com.badlogic.ashley.core.Component
import ctmn.petals.unit.buff.Buff
import ctmn.petals.utils.CopyableComponent

//TODO make strings list or smth
data class BuffsComponent(var buffs: ArrayList<Buff> = ArrayList()) : Component, CopyableComponent {
    override fun makeCopy(): Component {
        return copy()
    }
}