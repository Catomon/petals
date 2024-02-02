package ctmn.petals.playscreen.listeners

import ctmn.petals.player.Player
import ctmn.petals.playstage.PlayStage
import ctmn.petals.playscreen.events.PlayStageEvent
import ctmn.petals.playscreen.events.PlayStageListener
import com.badlogic.gdx.scenes.scene2d.Event


/* Should be added to PlayStage */
open class TurnsCycleListener(val turnCycles: Int = 1, var action: ((PlayStage) -> Unit)? = null) : PlayStageListener() {

    constructor(triggerPlayerId: Int, turnCycles: Int = 1, action: ((PlayStage) -> Unit)? = null) : this(turnCycles, action) {
        this.triggerPlayerId = triggerPlayerId
    }

    var mode = Mode.ONCE_AND_REMOVE

    enum class Mode {
        ONCE, ONCE_AND_REMOVE, REPEAT
    }

    var cyclesLeft = turnCycles

    var triggerPlayerId = -1

    override fun handle(event: Event): Boolean {
        if (event is TurnCycleEvent) {
            if (triggerPlayerId == -1)
                triggerPlayerId = event.lastPlayer.id

            if (event.nextPlayer.id == triggerPlayerId) {
                cyclesLeft--
                if (cyclesLeft == 0) {
                    action?.invoke(playStage)

                    when (mode) {
                        Mode.ONCE -> {}
                        Mode.REPEAT -> cyclesLeft = turnCycles
                        else -> playStage.removeListener(this)
                    }
                }
            }
        }

        return false
    }

    class TurnCycleEvent(val lastPlayer: Player, val nextPlayer: Player, val turnCycleTime: Float) : PlayStageEvent()
}