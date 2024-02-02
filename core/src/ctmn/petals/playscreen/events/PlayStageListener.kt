package ctmn.petals.playscreen.events

import ctmn.petals.playstage.PlayStage
import com.badlogic.gdx.scenes.scene2d.EventListener

abstract class PlayStageListener : EventListener {
    open lateinit var playStage: PlayStage

    open fun onPlayStage() {

    }
}