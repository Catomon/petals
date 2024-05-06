package ctmn.petals.playscreen.tasks

import com.badlogic.gdx.scenes.scene2d.Event
import com.badlogic.gdx.scenes.scene2d.EventListener
import ctmn.petals.player.Player
import ctmn.petals.playscreen.PlayScreen
import ctmn.petals.playscreen.events.ActionCompletedEvent
import ctmn.petals.playscreen.events.GameOverEvent
import ctmn.petals.playscreen.events.UnitDiedEvent
import ctmn.petals.playstage.getUnitsOfPlayer
import ctmn.petals.unit.playerId
import ctmn.petals.utils.log

class KeepPlayerUnitsAlive(val player: Player) : Task() {

    private val playStage get() = playScreen.playStage

    init {
        description = "Keep your units alive"
    }

    val playStageL = object : EventListener {
        override fun handle(event: Event?): Boolean {
            if (event is ActionCompletedEvent) {
                enemyDown = playStage.getUnitsOfPlayer(player).size == 0
            }

            if (event is UnitDiedEvent) {
                if (event.unit.playerId == 1) {
                    complete(state = State.FAILED)
                    log("task failed")
                }
            }

            if (event is GameOverEvent) {
                if (state != State.FAILED)
                    complete()
            }

            return false
        }
    }
    var enemyDown = false

    override fun update(delta: Float) {
        super.update(delta)

        if (!isCompleted) {
            if (playScreen.isGameOver && enemyDown && state != State.FAILED)
                complete()
        }
    }

    override fun onBegin(playScreen: PlayScreen) {
        super.onBegin(playScreen)

        playStage.addListener(playStageL)
    }

    override fun onCompleted() {
        super.onCompleted()

        playStage.removeListener(playStageL)
    }
}