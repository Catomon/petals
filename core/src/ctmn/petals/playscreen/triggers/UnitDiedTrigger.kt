package ctmn.petals.playscreen.triggers

import com.badlogic.gdx.scenes.scene2d.Event
import com.badlogic.gdx.scenes.scene2d.EventListener
import kotlin.reflect.KClass

class EventTrigger<T : Event>(private val eventClass: KClass<out T>) : Trigger() {

    var lastEvent: T? = null

    private var eventFired = false

    private val listener = EventListener {
        if (it::class == eventClass) {
            eventFired = true

            lastEvent = it as T?
        }

        false
    }

    override fun check(delta: Float): Boolean {
        if (eventFired) {
            if (removeOnTrigger)
                playScreen.playStage.removeListener(listener)
            else
                eventFired = false

            return true
        }

        return false
    }

    override fun onAdded() {
        super.onAdded()

        playScreen.playStage.addListener(listener)
    }
}