package ctmn.petals.playscreen.tasks

import ctmn.petals.playscreen.PlayScreen
import ctmn.petals.playscreen.events.NextTurnEvent
import ctmn.petals.utils.addOneTimeListener

class EndTurnTask : Task() {

    override var description: String? = "Press the End Turn button"

    override fun onBegin(playScreen: PlayScreen) {
        super.onBegin(playScreen)

        playScreen.playStage.addOneTimeListener<NextTurnEvent> {
            isCompleted = true

            return@addOneTimeListener true
        }
    }

    override fun onCompleted() {
        super.onCompleted()
    }
}