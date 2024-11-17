package ctmn.petals.playscreen.seqactions

import ctmn.petals.playscreen.PlayScreen
import ctmn.petals.playscreen.events.*
import com.badlogic.gdx.Gdx
import com.badlogic.gdx.utils.Array
import com.badlogic.gdx.utils.Queue

class SeqActionManager(val playScreen: PlayScreen) {

    val actionList = Array<SeqAction>()
    val actionQueue = Queue<SeqAction>()

    private var currentSeqAction: SeqAction? = null

    val currentAction get() = currentSeqAction

    val hasActions get() = !actionList.isEmpty || !actionQueue.isEmpty || currentSeqAction != null
    val isQueueEmpty get() = actionQueue.isEmpty

    fun update(deltaTime: Float) {
        //process list
        for (action in actionList) {
            action.update(deltaTime)
            if (action.isDone) {
                actionList.removeValue(action, false)

                playScreen.fireEvent(ActionCompletedEvent(action))
            }
        }

        //process queue
        if (!actionQueue.isEmpty) {
            val queueAction = actionQueue.first()

            if (currentSeqAction == null) {
                val started = start(queueAction!!)

                if (!started) {
                    actionQueue.removeValue(queueAction, false)
                    return
                }

                currentSeqAction = queueAction
            }

            currentSeqAction!!.update(deltaTime)
            if (currentSeqAction!!.isDone) {
                actionQueue.removeValue(currentSeqAction, false)

                playScreen.fireEvent(ActionCompletedEvent(currentSeqAction!!))

                currentSeqAction = null
                if (!actionQueue.isEmpty) {
                    currentSeqAction = actionQueue.first()

                    start(currentSeqAction!!)
                }
            }
        }
    }

    private fun start(action: SeqAction) : Boolean {

        action.isDone = false

        val started = action.onStart(playScreen)

        if (started)
            playScreen.fireEvent(ActionStartedEvent(action))
        else
            Gdx.app.log("[ActionManager]", "Action.onStart() returned false: $action")

        return started
    }

    fun getNextInQueue() : SeqAction? {
        return actionQueue.firstOrNull()
    }

    fun queueAction(action: SeqAction) : SeqAction {
        action.playScreen = playScreen

        actionQueue.addLast(action)

        playScreen.fireEvent(ActionQueuedEvent(action))

        return action
    }

    fun queueFirst(action: SeqAction) : SeqAction {
        action.playScreen = playScreen

        actionQueue.addFirst(action)

        playScreen.fireEvent(ActionQueuedEvent(action))

        return action
    }

    fun addAction(action: SeqAction) : Boolean {
        playScreen.fireEvent(ActionAddedEvent(action))

        action.playScreen = playScreen

        val started = start(action)

        if (started)
            actionList.add(action)

        return started
    }

    fun clear() {
        actionList.clear()
        actionQueue.clear()
        currentSeqAction = null
    }
}
