package ctmn.petals.playscreen.commands

import ctmn.petals.playscreen.PlayScreen
import ctmn.petals.playscreen.events.CommandAddedEvent
import ctmn.petals.playscreen.events.CommandExecutedEvent
import com.badlogic.gdx.Gdx
import com.badlogic.gdx.utils.Queue
import ctmn.petals.utils.logErr
import ctmn.petals.widgets.addNotifyWindow

class CommandManager(val playScreen: PlayScreen) {

    private val commandQueue = Queue<Command>()

    var stop = false

    var onCommand: ((Command) -> Unit)? = null

    private val commandHistory = Queue<Command>()
    
    val isQueueEmpty get() = commandQueue.isEmpty

    fun update(delta: Float) {
        // process queue
        while (!commandQueue.isEmpty && !stop && playScreen.actionManager.isQueueEmpty) {
            val queueCommand = commandQueue.first()
            commandQueue.removeValue(queueCommand, false)

            try {
                execute(queueCommand!!)
            } catch (e: Exception) {
                e.printStackTrace()
                stop = true
                playScreen.guiStage.addNotifyWindow(e.message ?: "null", "CommandMgr. exception", action = {
                    stop = false
                })
            }
        }
    }

    fun execute(command: Command) {
        if (!command.canExecute(playScreen)) {
            // it will happen sometimes cuz of Bot players
            logErr("Command ${command::class.simpleName} cannot be executed (command.canExecute() returns false)")
            return
        }

        val done = command.execute(playScreen)

        if (done) {
            commandHistory.addLast(command)
            onCommand?.invoke(command)

            playScreen.fireEvent(CommandExecutedEvent(command))
        } else
            Gdx.app.log(javaClass.simpleName, "Command execute failure: ${command.javaClass.simpleName}")
    }

    fun queueCommand(command: Command, playerId: Int? = null) : Boolean {
        if (!command.canExecute(playScreen)) {
            Gdx.app.error("CommandManager.queueCommand", "Command cannot be executed: $command")
            return false
        }

        if (playerId != null)
            command.playerId = playerId
        else
            if (command.playerId == -1)
                command.playerId = playScreen.localPlayer.id

        if (stop && commandQueue.size > 1)
            return false

        commandQueue.addLast(command)

        playScreen.fireEvent(CommandAddedEvent(command))

        return true
    }

    fun clearQueue() {
        commandQueue.clear()
    }

    fun getNextInQueue() : Command? = commandQueue.firstOrNull()
}
