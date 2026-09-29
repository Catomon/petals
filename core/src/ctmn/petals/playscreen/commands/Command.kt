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
     * True for a command whose outcome depends on a random roll.
     *
     * Such a command must not run on a client before the server has: the client would draw its own
     * numbers and apply its own damage and deaths. The online screen sends these and applies them
     * when they come back, with the server's rolls attached. See [Battle.roll].
     */
    open val rollsDice: Boolean get() = false

    /**
     * The numbers the server drew while running this command, filled in by the side that ran it
     * first and replayed by everybody else.
     */
    var rolls: List<Int> = emptyList()

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
