package ctmn.petals.playscreen.commands

import ctmn.petals.player.Player
import ctmn.petals.playscreen.PlayScreen

class EndTurnCommand(var turnPlayerId: Int) : Command() {

    constructor(player: Player) : this(player.id)

    override fun canExecute(playScreen: PlayScreen): Boolean {
        return playScreen.turnManager.currentPlayer.id == turnPlayerId
    }

    override fun execute(playScreen: PlayScreen): Boolean {
        if (!canExecute(playScreen)) return false

        if (playScreen.guiStage.currentState == playScreen.guiStage.myTurn)
            playScreen.guiStage.currentState = playScreen.guiStage.endTurn

        playScreen.turnManager.nextTurn()

        return true
    }
}