package ctmn.petals.playscreen.seqactions

import ctmn.petals.playscreen.PlayScreen

abstract class SeqAction {

    lateinit var playScreen: PlayScreen

    var isDone: Boolean = false

    var lifeTime = 0f

    open fun update(deltaTime: Float) {
        lifeTime -= deltaTime
        if (lifeTime <= 0)
            isDone = true
    }

    open fun onStart(playScreen: PlayScreen) : Boolean = true
}
