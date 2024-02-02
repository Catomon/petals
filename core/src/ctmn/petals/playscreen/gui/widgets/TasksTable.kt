package ctmn.petals.playscreen.gui.widgets

import ctmn.petals.playscreen.events.TaskBeginEvent
import ctmn.petals.playscreen.events.TaskCompletedEvent
import ctmn.petals.playscreen.events.TaskUpdatedEvent
import ctmn.petals.playscreen.gui.PlayGUIStage
import ctmn.petals.playscreen.tasks.Task
import ctmn.petals.widgets.newLabel
import com.badlogic.gdx.scenes.scene2d.Stage
import com.kotcrab.vis.ui.widget.VisLabel
import com.kotcrab.vis.ui.widget.VisTable

class TasksTable : VisTable() {

    private val showAllTasks = false

    private fun addTask(task: Task, description: String) {
        val label = newLabel(description, "task")
        label.userObject = task
        add(label).top().left()
        row()

        pack()
    }

    override fun setStage(stage: Stage?) {
        super.setStage(stage)

        if (stage == null)
            return

        check(stage is PlayGUIStage)

        stage.addListener {
            when (it) {
                is TaskBeginEvent -> {
                    val description = it.task.description
                    if (description != null) {
                        addTask(it.task, description)
                    } else
                        if (showAllTasks) {
                            addTask(it.task, it.task.toString())
                        }
                }

                is TaskUpdatedEvent -> {
                    for (taskCell in this.cells) {

                        if (taskCell.actor !is VisLabel) continue

                        val taskLabel = taskCell.actor as VisLabel
                        if (taskLabel.userObject == it.task)
                            taskLabel.setText(it.task.description)
                    }

                    pack()
                }

                is TaskCompletedEvent -> {
                    for (taskCell in this.cells) {

                        if (taskCell.actor !is VisLabel) continue

                        val taskLabel = taskCell.actor as VisLabel
                        if(taskLabel.userObject == it.task) {
                            removeActor(taskLabel)

                            //TODO animation based on completion result
                        }
                    }
                }
            }

            false
        }
    }
}