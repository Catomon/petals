package ctmn.petals.playscreen.commands

import com.badlogic.gdx.Gdx
import com.badlogic.gdx.utils.Queue
import ctmn.petals.sim.Battle
import ctmn.petals.utils.logErr

class CommandManager(val battle: Battle) {

    private val commandQueue = Queue<Command>()

    var stop = false

    var onCommand: ((Command) -> Unit)? = null

    private val commandHistory = Queue<Command>()

    val isQueueEmpty get() = commandQueue.isEmpty

    fun update(delta: Float) {
        // process queue
        while (!commandQueue.isEmpty && !stop && !battle.isActionsBusy) {
            val queueCommand = commandQueue.first()
            commandQueue.removeValue(queueCommand, false)

            try {
                execute(queueCommand!!)
            } catch (e: Exception) {
                e.printStackTrace()
                stop = true
                Gdx.app.error("CommandManager", "Command exception: ${e.message}")
            }
        }
    }

    fun execute(command: Command) {
        if (!command.canExecute(battle)) {
            // it will happen sometimes cuz of Bot players
            logErr("Command ${command::class.simpleName} cannot be executed (command.canExecute() returns false)")
            return
        }

        val done = command.execute(battle)

        if (done) {
            commandHistory.addLast(command)
            onCommand?.invoke(command)

            battle.fireEvent(ctmn.petals.playscreen.events.CommandExecutedEvent(command))
        } else
            Gdx.app.log(javaClass.simpleName, "Command execute failure: ${command.javaClass.simpleName}")
    }

    fun queueCommand(command: Command, playerId: Int? = null) : Boolean {
        if (!command.canExecute(battle)) {
            Gdx.app.error("CommandManager.queueCommand", "Command cannot be executed: $command")
            return false
        }

        if (playerId != null)
            command.playerId = playerId
        else if (command.playerId == -1 && battle.hasLocalPlayer)
            command.playerId = battle.localPlayer.id

        if (stop && commandQueue.size > 1)
            return false

        commandQueue.addLast(command)

        battle.fireEvent(ctmn.petals.playscreen.events.CommandAddedEvent(command))

        return true
    }

    fun clearQueue() {
        commandQueue.clear()
    }

    fun getNextInQueue() : Command? = commandQueue.firstOrNull()
}
