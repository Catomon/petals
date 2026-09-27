package ctmn.petals.playscreen.commands

import ctmn.petals.sim.Battle

/**
 * Asks the server to resolve a move instead of applying one.
 *
 * The client sends this without applying anything locally when fog could make its own
 * reachability verdict wrong: a hidden enemy on or next to the route changes what the server
 * allows. The server resolves it against the full state, executes a real [MoveUnitCommand]
 * (possibly shortened, with a reason), and broadcasts that. The broadcast plays on every
 * client, including the sender, like any other command.
 *
 * Never queued or executed: legality is the server's whole point here.
 */
class TryMoveCommand(val unitId: String = "", val tileX: Int = -1, val tileY: Int = -1) : Command() {

    override fun canExecute(battle: Battle): Boolean = false

    override fun execute(battle: Battle): Boolean = false
}
