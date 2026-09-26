package ctmn.petals.playscreen.commands

import ctmn.petals.sim.Battle

abstract class Command {

    open var playerId: Int = -1

    open fun canExecute(battle: Battle) : Boolean = true

    abstract fun execute(battle: Battle) : Boolean
}
