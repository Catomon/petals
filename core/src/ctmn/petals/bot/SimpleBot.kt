package ctmn.petals.bot

import com.badlogic.gdx.Gdx
import ctmn.petals.player.Player
import ctmn.petals.playscreen.PlayScreen
import ctmn.petals.playscreen.commands.Command
import ctmn.petals.utils.logErr
import ctmn.petals.widgets.addNotifyWindow
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

    private var currentCommand: Command? = null

    private var waitingForActionEndTime = 0f

    override fun update(delta: Float) {
        if (isDone) return

        elapsedTime += delta

        if (playScreen.actionManager.hasActions) {
            if (!didIPrintDebug) {
                Gdx.app.log(
                    this::class.simpleName,
                    "Last thinking elapsed time: ${lastThinkingElapsedTime / 1_000_000}"
                )
                Gdx.app.log(this::class.simpleName, "Waiting for action complete...")

                didIPrintDebug = true
            }

            waitingForActionEndTime += delta

            if (waitingForActionEndTime > 10f) {
                logErr("Action takes too long, removing: " + (playScreen.actionManager.actionList + playScreen.actionManager.actionQueue).joinToString(", "))
                if (playScreen.actionManager.getNextInQueue() != null)
                    playScreen.actionManager.getNextInQueue()?.isDone = true
                else if (!playScreen.actionManager.actionList.isEmpty)
                    playScreen.actionManager.actionList.forEach { it.isDone = true }
            }

            elapsedTime = 0f
            return
        } else {
            waitingForActionEndTime = 0f
        }

        if (elapsedTime < idleTime) return

        if (currentCommand != null) {
            val isCommandExecuted = try {
                playScreen.commandManager.queueCommand(currentCommand!!, playerID)
            } catch (e: Exception) {
                isDone = true

                Gdx.app.postRunnable {
                    e.printStackTrace()
                    playScreen.commandManager.stop = true
                    playScreen.guiStage.addNotifyWindow(e.message ?: "null", "SimpleBot queueCommand exc.", action = {
                        playScreen.commandManager.stop = false
                    })
                }

                false
            }
            if (isCommandExecuted) commandExecuted()
            currentCommand = null
        }

        if (thinkingThread?.isAlive != true && currentCommand == null) {
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

                val command =
                    try {
                        simpleAI.makeCommand()
                    } catch (e: Exception) {
                        isDone = true

                        Gdx.app.postRunnable {
                            e.printStackTrace()
                            playScreen.commandManager.stop = true
                            playScreen.guiStage.addNotifyWindow(
                                e.message ?: "null",
                                "SimpleBot makeCommand exc.",
                                action = {
                                    playScreen.commandManager.stop = false
                                })
                        }

                        null
                    }

                val endTime = System.nanoTime()
                lastThinkingElapsedTime = endTime - startTime

                if (command != null) {
                    if (command.canExecute(playScreen)) {
                        currentCommand = command
                    }
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