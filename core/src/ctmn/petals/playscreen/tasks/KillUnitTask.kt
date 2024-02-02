package ctmn.petals.playscreen.tasks

import ctmn.petals.unit.UnitActor
import ctmn.petals.unit.isAlive

class KillUnitTask(val unit: UnitActor) : Task() {
    override var description: String? = "Eliminate enemy unit"

    override fun update(delta: Float) {
        isCompleted = !unit.isAlive()
    }
}