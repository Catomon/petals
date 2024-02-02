package ctmn.petals.effects

import com.badlogic.gdx.scenes.scene2d.Stage
import com.badlogic.gdx.scenes.scene2d.actions.Actions
import com.kotcrab.vis.ui.widget.VisLabel

class FloatingLabelAnimation(text: String, style: String) : VisLabel(text, style) {

    private val duration = 1f

    init {
        setFontScale(0.25f)
    }

    fun position(x: Float, y: Float) : VisLabel {
        setPosition(x, y)

        return this
    }

    override fun setStage(stage: Stage?) {
        super.setStage(stage)

        if (stage != null)
            addAction(Actions.sequence(Actions.moveTo(x, y + 6, duration), Actions.removeActor()))
    }
}