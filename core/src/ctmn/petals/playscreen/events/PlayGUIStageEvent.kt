package ctmn.petals.playscreen.events

import ctmn.petals.playscreen.gui.PlayGUIStage
import com.badlogic.gdx.scenes.scene2d.Event
import com.badlogic.gdx.scenes.scene2d.Stage

abstract class PlayGUIStageEvent : Event() {

    override fun setStage(stage: Stage?) {
        if (stage != null && stage !is PlayGUIStage) throw IllegalArgumentException("stage != PlayGUIStage")

        super.setStage(stage)
    }
}