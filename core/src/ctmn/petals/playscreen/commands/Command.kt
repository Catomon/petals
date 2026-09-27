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

    /**
     * Why [canExecute] said no, in words and with the numbers behind it.
     *
     * Only for logging. A client replaying the server's commands sometimes cannot run one that the
     * server accepted, and "cannot replay MoveUnitCommand" on its own says nothing about which of
     * its checks disagreed. Returns null when there is nothing worth adding.
     */
    open fun explainFailure(battle: Battle): String? = null

    abstract fun execute(battle: Battle) : Boolean
}
