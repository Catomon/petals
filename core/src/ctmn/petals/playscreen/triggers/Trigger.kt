package ctmn.petals.playscreen.triggers

import ctmn.petals.playscreen.PlayScreen

abstract class Trigger {

    open var onTrigger: ((PlayScreen) -> Unit)? = null

    var removeOnTrigger = true

    lateinit var playScreen: PlayScreen

    abstract fun check(delta: Float) : Boolean

    open fun onAdded() {

    }

    fun trigger(onTrigger: ((PlayScreen) -> Unit)?) : Trigger {
        this.onTrigger = onTrigger

        return this
    }

    fun dontRemoveOnTrigger() : Trigger {
        removeOnTrigger = false

        return this
    }
}