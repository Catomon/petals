package ctmn.petals.playscreen

import ctmn.petals.playscreen.events.CommandAddedEvent
import ctmn.petals.playscreen.events.CommandExecutedEvent
import com.badlogic.gdx.scenes.scene2d.Event
import com.badlogic.gdx.scenes.scene2d.EventListener
import com.badlogic.gdx.utils.Logger

class EventLogger(val playScreen: PlayScreen) : EventListener {

    private val log = Logger("EventLogger", Logger.DEBUG)

    init {
        playScreen.playStage.addListener(this)
    }

    override fun handle(e: Event): Boolean {
        if (!playScreen.initView) return false

        when (e) {
            is CommandExecutedEvent -> log.info(e.command.javaClass.simpleName + " executed.")

            is CommandAddedEvent -> log.info(e.command.javaClass.simpleName + " added.")
        }

        return false
    }
}