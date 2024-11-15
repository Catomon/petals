package ctmn.petals.effects

import com.badlogic.gdx.graphics.g2d.Batch
import com.badlogic.gdx.scenes.scene2d.Stage
import com.badlogic.gdx.scenes.scene2d.actions.Actions
import com.kotcrab.vis.ui.widget.VisLabel

class FloatingUpLabel(text: String, private val moveBy: Float = 60f, ) : VisLabel(text, "default") {

    private val duration = 1f

    init {
        setFontScale(0.5f)
        setPosition(3f, 100f)
    }

    override fun setStage(stage: Stage?) {
        super.setStage(stage)

        addAction(Actions.sequence(Actions.moveTo(x, y + moveBy, duration + 0.5f), Actions.removeActor()))
        addAction(Actions.sequence(Actions.delay(duration), Actions.fadeOut(0.5f)))
    }
}