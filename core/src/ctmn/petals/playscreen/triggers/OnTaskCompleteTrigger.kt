package ctmn.petals.playscreen.triggers

import ctmn.petals.playscreen.tasks.Task

class OnTaskCompleteTrigger(val task: Task) : Trigger() {
    override fun check(delta: Float): Boolean {
        return task.isCompleted
    }
}