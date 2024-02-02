package ctmn.petals.playscreen.commands

import ctmn.petals.playscreen.PlayScreen

abstract class Command {

    open var playerId: Int = -1

    open fun canExecute(playScreen: PlayScreen) : Boolean = true

    abstract fun execute(playScreen: PlayScreen) : Boolean
}
