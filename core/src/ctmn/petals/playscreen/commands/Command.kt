package ctmn.petals.playscreen.commands

import ctmn.petals.sim.Battle

abstract class Command {

    open var playerId: Int = -1

    /**
     * True for a command that arrived from the server rather than being made by this client.
     * The online client must not send those back, or a command the server forced, such as an
     * end turn from the turn timer, would be replayed as a fresh player action.
     */
    open var fromServer: Boolean = false

    open fun canExecute(battle: Battle) : Boolean = true

    abstract fun execute(battle: Battle) : Boolean
}
