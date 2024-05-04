package ctmn.petals.bot

import com.badlogic.gdx.Gdx
import ctmn.petals.player.Player
import ctmn.petals.playscreen.PlayScreen
import ctmn.petals.playscreen.commands.Command
import ctmn.petals.utils.printLess
import kotlin.concurrent.thread

class ScoreBot(player: Player, playScreen: PlayScreen) : Bot(player, playScreen) {

    val scoreAI = ScoreAI(player, playScreen)

    private val idleTime = 0.5f
    private var elapsedTime = 0f

    private var currentCommand: Command? = null

    private var didISayWaiting = false
    private var didISayNext = false

    private var moveCamera = true

    private var thinking = false
    private var isCommandExecuted = false
    private var noCommands = false

    private var thinkingThread: Thread? = null

    override fun update(delta: Float) {
        if (isDone) return

        elapsedTime += delta

        if (playScreen.actionManager.hasActions) {
            if (!didISayWaiting) {
                Gdx.app.log(this::class.simpleName, "Waiting for action complete...")

                didISayWaiting = true
            }

            elapsedTime = 0f
            return
        }

        if (elapsedTime < idleTime) return

        if (thinkingThread?.isAlive != true) {
            startThinking()
        }

        isDone = noCommands && elapsedTime > 1 && playScreen.actionManager.isQueueEmpty

        if (isDone) {
            thinking = false
            isCommandExecuted = false
            noCommands = false
            elapsedTime = 0f
            didISayWaiting = false
            didISayNext = false
        }
    }

    override fun onEnd() {
        super.onEnd()


    }

    private fun commandExecuted() {
        elapsedTime = 0f
        isCommandExecuted = false
        didISayWaiting = false
    }

    private fun startThinking() {
        printLess("Thinking...")
        thinking = true
        thinkingThread = thread {
            try {

                val startTime = System.nanoTime()

                val command = scoreAI.makeCommand()

                val endTime = System.nanoTime()
                val elapsedTime = endTime - startTime

                println("AI elapsed time: ${elapsedTime / 1_000_000} milliseconds")

                Gdx.app.log(this::class.simpleName, "Next Command...")

                if (command != null) {
                    isCommandExecuted = command.canExecute(playScreen) && playScreen.commandManager.queueCommand(command)
                    if (isCommandExecuted) commandExecuted()
                } else
                    noCommands = true
            } catch (e: Exception) {
                e.printStackTrace()
            } finally {
                thinking = false
            }
        }
    }
}