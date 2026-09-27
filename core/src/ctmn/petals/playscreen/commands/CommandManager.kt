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
            // It will happen sometimes because of bot players, and for a command from the server it
            // means our copy of the state disagrees with the server's. The command can say which of
            // its checks failed, so ask it rather than logging a bare class name.
            val because = runCatching { command.explainFailure(battle) }.getOrNull()
            logErr("Command ${command::class.simpleName} cannot be executed${because?.let { ": $it" } ?: ""}")
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
        // A command the server already accepted is not ours to refuse here.
        //
        // This check asks "can this run right now", but a queued command has not run yet, so a
        // command arriving from the network is judged against a state one or more commands behind
        // the server's. That is how a client rejected a perfectly legal move: the end of turn that
        // refilled the unit's action points was still sitting in the queue in front of it, so the
        // move looked like it had no action points left. It is checked again in [execute], by which
        // point the queue in front of it has drained and the state is right.
        //
        // A command this client invented is still checked here, because that is where a local
        // action that cannot happen gets turned away.
        if (!command.fromServer && !command.canExecute(battle)) {
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
