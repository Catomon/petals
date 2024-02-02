package ctmn.petals.playscreen.triggers

import ctmn.petals.unit.UnitActor
import ctmn.petals.unit.isAlive
import com.badlogic.gdx.utils.Array

class UnitsDiedTrigger(val units: Array<UnitActor>) : Trigger() {

    constructor(unit: UnitActor) : this(Array<UnitActor>().apply { add(unit) })

    override fun check(delta: Float): Boolean {
        units.forEach {
            if (it.isAlive()) return false
        }

        return true
    }
}