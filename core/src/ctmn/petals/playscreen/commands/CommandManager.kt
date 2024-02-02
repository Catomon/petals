package ctmn.petals.playscreen.commands

import ctmn.petals.playscreen.PlayScreen
import ctmn.petals.playscreen.events.CommandAddedEvent
import ctmn.petals.playscreen.events.CommandExecutedEvent
import com.badlogic.gdx.Gdx
import com.badlogic.gdx.utils.Queue

class CommandManager(val playScreen: PlayScreen) {

    private val commandQueue = Queue<Command>()

    var stop = false

    var onCommand: ((Command) -> Unit)? = null

    private val commandHistory = Queue<Command>()
    
    val isQueueEmpty get() = commandQueue.isEmpty

    fun update(deltaTime: Float) {
        // process queue
        while (!commandQueue.isEmpty && !stop && playScreen.actionManager.isQueueEmpty) {
            val queueCommand = commandQueue.first()
            commandQueue.removeValue(queueCommand, false)
            execute(queueCommand!!)
        }
    }

    fun execute(command: Command) {
        if (!command.canExecute(playScreen))
            Gdx.app.error(javaClass.simpleName, "Executing ${command.javaClass.simpleName} while its canExecute() returns false")

        val done = command.execute(playScreen)

        if (done) {
            commandHistory.addLast(command)
            onCommand?.invoke(command)

            playScreen.fireEvent(CommandExecutedEvent(command))
        } else
            Gdx.app.log(javaClass.simpleName, "Command execute failure: ${command.javaClass.simpleName}")
    }

    fun queueCommand(command: Command, playerId: Int? = null) : Boolean {
//        if (!command.canExecute(playScreen))
//            return false

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
