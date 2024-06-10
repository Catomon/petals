package ctmn.petals.bot

import ctmn.petals.player.Player
import ctmn.petals.playscreen.PlayScreen
import ctmn.petals.playstage.PlayStage

class EasyBot(player: Player, playScreen: PlayScreen) : Bot(player, playScreen) {

//    private lateinit var enemyUnits:

    override fun levelCreated(playStage: PlayStage) {
        super.levelCreated(playStage)


    }

    override fun update(delta: Float) {
        TODO("Not yet implemented")
    }

    private fun onCommand() {

    }

    override fun onStart() {
        super.onStart()
    }

    override fun onEnd() {
        super.onEnd()
    }
}