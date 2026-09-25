package ctmn.petals.playscreen

import ctmn.petals.playscreen.events.CommandAddedEvent
import ctmn.petals.playscreen.events.CommandExecutedEvent
import ctmn.petals.utils.GameLog
import com.badlogic.gdx.scenes.scene2d.Event
import com.badlogic.gdx.scenes.scene2d.EventListener

class EventLogger(val playScreen: PlayScreen) : EventListener {

    init {
        playScreen.playStage.addListener(this)
    }

    override fun handle(e: Event): Boolean {
        if (!playScreen.initView) return false

        when (e) {
            is CommandExecutedEvent -> GameLog.verbose("EventLogger", e.command.javaClass.simpleName + " executed.")

            is CommandAddedEvent -> GameLog.verbose("EventLogger", e.command.javaClass.simpleName + " added.")
        }

        return false
    }
}
