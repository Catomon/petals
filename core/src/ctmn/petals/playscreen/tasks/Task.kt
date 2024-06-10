package ctmn.petals.playscreen.tasks

import ctmn.petals.playscreen.PlayScreen

abstract class Task {

    enum class State {
        IN_PROCESS,
        SUCCEEDED,
        FAILED,
        CANCELED
    }

    open var description: String? = null
    var state = State.IN_PROCESS
    var isCompleted = false

    var stopCommands = false
    var isForcePlayerToComplete = false

    lateinit var playScreen: PlayScreen

    open fun update(delta: Float) {

    }

    open fun onBegin(playScreen: PlayScreen) {
        if (stopCommands) playScreen.commandManager.stop =  true
    }

    open fun complete(state: State = State.SUCCEEDED) {
        isCompleted = true

        this.state = state
    }

    open fun onCompleted() {
        if (stopCommands) playScreen.commandManager.stop = false

        if (state == State.IN_PROCESS)
            this.state = State.SUCCEEDED
    }

    fun description(string: String? = null) : Task {
        description = string

        return this
    }
}