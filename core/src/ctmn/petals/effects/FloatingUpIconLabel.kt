package ctmn.petals.effects

import com.badlogic.gdx.graphics.Color
import com.badlogic.gdx.graphics.g2d.Batch
import com.badlogic.gdx.scenes.scene2d.Stage
import com.badlogic.gdx.scenes.scene2d.actions.Actions
import com.badlogic.gdx.scenes.scene2d.ui.Container
import com.badlogic.gdx.scenes.scene2d.ui.HorizontalGroup
import com.badlogic.gdx.utils.Align
import com.kotcrab.vis.ui.widget.VisImage
import com.kotcrab.vis.ui.widget.VisLabel

class FloatingUpIconLabel(val text: String, val iconName: String, val moveBy: Float = 60f) : HorizontalGroup() {

    private val duration = 1f
    val label = VisLabel(text, "font_5").also { it.color = Color.SKY; it.setFontScale(0.5f) }
    
    init {
        addActor(label)
        label.setFontScale(0.33f)
        align(Align.center)
        addActor(Container<VisImage>(VisImage(iconName)).also { it.size(8f).padBottom(4f) })
        setPosition(3f, 100f)
    }

    override fun setStage(stage: Stage?) {
        if (stage != null) {
            addAction(Actions.sequence(Actions.moveTo(x, y + moveBy, duration + 0.5f), Actions.removeActor()))
            addAction(Actions.sequence(Actions.delay(duration), Actions.fadeOut(0.5f)))
        }

        super.setStage(stage)
    }
}