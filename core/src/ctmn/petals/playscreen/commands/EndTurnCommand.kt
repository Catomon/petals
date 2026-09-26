package ctmn.petals.playscreen.commands

import ctmn.petals.player.Player
import ctmn.petals.sim.Battle

class EndTurnCommand(var turnPlayerId: Int) : Command() {

    constructor(player: Player) : this(player.id)

    override fun canExecute(battle: Battle): Boolean {
        return battle.turnManager.currentPlayer.id == turnPlayerId
    }

    override fun execute(battle: Battle): Boolean {
        if (!canExecute(battle)) return false

        battle.turnManager.nextTurn()

        return true
    }
}
