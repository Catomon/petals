package ctmn.petals.playscreen.events

import com.badlogic.gdx.scenes.scene2d.Event
import com.badlogic.gdx.scenes.scene2d.Stage

abstract class PlayStageEvent : Event() {

    override fun setStage(stage: Stage?) {
        //if (stage != null && stage !is PlayStage) throw IllegalArgumentException("stage != PlayStage")

        super.setStage(stage)
    }
}