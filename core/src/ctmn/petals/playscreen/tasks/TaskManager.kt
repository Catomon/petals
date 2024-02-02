package ctmn.petals.playscreen.tasks

import ctmn.petals.playscreen.PlayScreen
import ctmn.petals.playscreen.events.TaskBeginEvent
import ctmn.petals.playscreen.events.TaskCompletedEvent
import com.badlogic.gdx.utils.Array
import com.badlogic.gdx.utils.Queue

class TaskManager(val playScreen: PlayScreen) {
    
    private val taskList = Array<Task>()
    private val taskQueue = Queue<Task>()

    private var currentQueueTask: Task? = null
    
    fun update(delta: Float) {
        for (task in taskList) {
            task.update(delta)
            if (task.isCompleted) {
                taskList.removeValue(task, false)
                completeTask(task)
            }
        }

        if (!taskQueue.isEmpty) {
            val task = taskQueue.first()

            if (currentQueueTask == null) {
                beginTask(task)
                currentQueueTask = task
            }

            task.update(delta)
            if (task.isCompleted) {
                taskQueue.removeValue(task, false)
                completeTask(task)
                currentQueueTask = null
                if (!taskQueue.isEmpty) {
                    beginTask(taskQueue.first())
                    currentQueueTask = task
                }
            }
        }
    }

    private fun beginTask(task: Task) {
        task.isCompleted = false

        task.onBegin(playScreen)

        playScreen.fireEvent(TaskBeginEvent(task))
    }

    fun completeTask() {
        if (currentQueueTask == null) return
        val currentQueueTask = currentQueueTask!!

        if (!currentQueueTask.isCompleted) {
            taskQueue.removeValue(currentQueueTask, false)

            currentQueueTask.isCompleted = true
        }

        currentQueueTask.onCompleted()

        playScreen.fireEvent(TaskCompletedEvent(currentQueueTask))
    }

    fun completeTasks() {
        for (task in taskList)
            completeTask(task)

        for (task in taskQueue)
            completeTask(task)
    }

    fun completeTask(task: Task) {
        if (!task.isCompleted) {
            taskQueue.removeValue(task, false)
            taskList.removeValue(task, false)

            task.isCompleted = true
        }

        task.onCompleted()

        playScreen.fireEvent(TaskCompletedEvent(task))
    }

    fun queueTask(task: Task) {
        task.playScreen = playScreen

        taskQueue.addLast(task)
    }

    fun addTask(task: Task) {
        task.playScreen = playScreen

        taskList.add(task)

        beginTask(task)
    }

    fun getTasks() : Array<Task> {
        val tasks = Array<Task>()

        tasks.addAll(taskList)
        taskQueue.forEach { tasks.add(it) }

        return tasks
    }
}