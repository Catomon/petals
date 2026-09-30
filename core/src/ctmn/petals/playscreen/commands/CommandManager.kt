package ctmn.petals.playscreen.commands

import com.badlogic.gdx.Gdx
import com.badlogic.gdx.utils.Queue
import ctmn.petals.sim.Battle
import ctmn.petals.utils.GameLog
import ctmn.petals.utils.logErr

class CommandManager(val battle: Battle) {

    private val commandQueue = Queue<Command>()

    var stop = false

    var onCommand: ((Command) -> Unit)? = null

    /**
     * Fired when a command that came from the server could not be run here. The two copies of the
     * state have parted, and the listener should ask for a fresh state rather than let every later
     * command fail in turn.
     */
    var onServerCommandFailed: ((Command) -> Unit)? = null

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

    /**
     * Runs a command and reports whether it ran.
     *
     * This is the one place a command executes, whether it came from the queue, from a client, or
     * from a bot.
     */
    fun execute(command: Command): Boolean {
        if (!command.canExecute(battle)) {
            // It will happen sometimes because of bot players, and for a command from the server it
            // means our copy of the state disagrees with the server's. The command can say which of
            // its checks failed, so ask it rather than logging a bare class name.
            val because = runCatching { command.explainFailure(battle) }.getOrNull()
            logErr("Command ${command::class.simpleName} cannot be executed${because?.let { ": $it" } ?: ""}")

            // A command the server accepted and we cannot run is a real difference between the two
            // copies, not fog. Whoever is listening asks for the state, so the difference is
            // repaired here rather than surfacing as a rejection several commands later.
            if (command.fromServer) onServerCommandFailed?.invoke(command)

            return false
        }

        val done = command.execute(battle)

        if (done) {
            commandHistory.addLast(command)
            onCommand?.invoke(command)

            battle.fireEvent(ctmn.petals.playscreen.events.CommandExecutedEvent(command))

            // The fingerprint of the state after this command, so two machines' logs can be put
            // side by side and the first command that applied differently stands out. Verbose, so
            // a normal run pays nothing for it.
            GameLog.verbose(
                "StateHash",
                "${battle.gameStateId} ${battle.stateHashReport()} <- ${command.javaClass.simpleName}"
            )
        } else {
            Gdx.app.log(javaClass.simpleName, "Command execute failure: ${command.javaClass.simpleName}")
            if (command.fromServer) onServerCommandFailed?.invoke(command)
        }

        return done
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
