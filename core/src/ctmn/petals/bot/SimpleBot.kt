package ctmn.petals.bot

import com.badlogic.gdx.Gdx
import ctmn.petals.player.Player
import ctmn.petals.playscreen.PlayScreen
import kotlin.concurrent.thread

class SimpleBot(player: Player, playScreen: PlayScreen) : Bot(player, playScreen) {

    val simpleAI = SimpleAI(player, playScreen)

    private val idleTime = 0.5f
    private var elapsedTime = 0f

    private var didIPrintDebug = false

    private var thinking = false
    private var isCommandExecuted = false
    private var noCommands = false

    private var thinkingThread: Thread? = null

    private var lastThinkingElapsedTime = 0L

    override fun update(delta: Float) {
        if (isDone) return

        elapsedTime += delta

        if (playScreen.actionManager.hasActions) {
            if (!didIPrintDebug) {
                Gdx.app.log(this::class.simpleName, "Last thinking elapsed time: ${lastThinkingElapsedTime/1_000_000}")
                Gdx.app.log(this::class.simpleName, "Waiting for action complete...")

                didIPrintDebug = true
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
            didIPrintDebug = false
        }
    }

    override fun onEnd() {
        super.onEnd()
    }

    private fun commandExecuted() {
        elapsedTime = 0f
        isCommandExecuted = false
        didIPrintDebug = false
    }

    private fun startThinking() {
        thinking = true
        thinkingThread = thread {
            try {

                val startTime = System.nanoTime()

                val command = simpleAI.makeCommand()

                val endTime = System.nanoTime()
                lastThinkingElapsedTime = endTime - startTime

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