package ctmn.petals.playscreen.gui.widgets

import com.badlogic.gdx.graphics.Color
import com.badlogic.gdx.math.Interpolation
import com.badlogic.gdx.math.Vector2
import ctmn.petals.playscreen.events.TaskBeginEvent
import ctmn.petals.playscreen.events.TaskCompletedEvent
import ctmn.petals.playscreen.events.TaskUpdatedEvent
import ctmn.petals.playscreen.gui.PlayGUIStage
import ctmn.petals.playscreen.tasks.Task
import ctmn.petals.widgets.newLabel
import com.badlogic.gdx.scenes.scene2d.Stage
import com.badlogic.gdx.scenes.scene2d.actions.Actions
import com.badlogic.gdx.scenes.scene2d.actions.AlphaAction
import com.badlogic.gdx.scenes.scene2d.actions.MoveByAction
import com.badlogic.gdx.scenes.scene2d.actions.MoveToAction
import com.badlogic.gdx.scenes.scene2d.actions.RemoveAction
import com.kotcrab.vis.ui.widget.VisLabel
import com.kotcrab.vis.ui.widget.VisTable
import ctmn.petals.actors.actions.OneAction

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

        stage.addListener { event ->
            when (event) {
                is TaskBeginEvent -> {
                    val description = event.task.description
                    if (description != null) {
                        addTask(event.task, description)
                    } else
                        if (showAllTasks) {
                            addTask(event.task, event.task.toString())
                        }
                }

                is TaskUpdatedEvent -> {
                    for (taskCell in this.cells) {

                        if (taskCell.actor !is VisLabel) continue

                        val taskLabel = taskCell.actor as VisLabel
                        if (taskLabel.userObject == event.task)
                            taskLabel.setText(event.task.description)
                    }

                    pack()
                }

                is TaskCompletedEvent -> {
                    for (taskCell in this.cells) {

                        if (taskCell.actor !is VisLabel) continue

                        val taskLabel = taskCell.actor as VisLabel
                        if (taskLabel.userObject == event.task) {
                            //removeActor(taskLabel)
                            if (event.task.state == Task.State.FAILED) {
                                taskLabel.setText(taskLabel.text.toString() + " X")
                                taskLabel.color = Color.RED
                            } else {
                                taskLabel.setText(taskLabel.text.toString() + " V")
                                taskLabel.color = Color.GREEN
                                taskLabel.addAction(
                                    Actions.sequence(
                                        AlphaAction().also {
                                            it.alpha = 0f
                                            it.duration = 2f
                                        },
//                                        MoveToAction().apply {
//                                            interpolation = Interpolation.slowFast
//                                            duration = 1f
//                                            setPosition(
//                                                taskLabel.x,// - taskLabel.width,
//                                                taskLabel.localToStageCoordinates(Vector2(0f, 0f)).y + height
//                                            )
//                                        },
                                        OneAction {
                                            val indexOfLabelCell = cells.indexOfFirst { it.actor == taskLabel }
                                            //cells.removeRange(indexOfLabelCell, indexOfLabelCell + 1)
                                            taskLabel.remove()
                                        }
                                    )
                                )
                            }
                        }
                    }
                }
            }

            false
        }
    }
}