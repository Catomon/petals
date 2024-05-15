package ctmn.petals.playscreen.tasks

import ctmn.petals.playscreen.PlayScreen
import ctmn.petals.playscreen.commands.Command
import kotlin.reflect.KClass

class ExecuteCommandTask(val commandClass: KClass<out Command>, forcePlayerToComplete: Boolean = false) : Task() {

    override var description: String? = "Press the End Turn button"

    private var commandPassed = false

    init {
        this.isForcePlayerToComplete = forcePlayerToComplete
    }

    override fun update(delta: Float) {
        if (isForcePlayerToComplete)
            playScreen.commandManager.getNextInQueue()?.also {
                if (it::class == commandClass) {
                    playScreen.commandManager.stop = false
                    commandPassed = true
                } else
                    playScreen.commandManager.clearQueue()
            }

        if (commandPassed && playScreen.actionManager.isQueueEmpty)
            isCompleted = true
    }

    override fun onBegin(playScreen: PlayScreen) {
        super.onBegin(playScreen)

        if (isForcePlayerToComplete) {
            playScreen.commandManager.stop = true
        }
    }
}