package ctmn.petals.effects

import com.badlogic.gdx.graphics.g2d.Batch
import com.badlogic.gdx.scenes.scene2d.actions.Actions
import com.kotcrab.vis.ui.widget.VisLabel

class FloatingUpLabel(text: String) : VisLabel(text, "font_5") {

    private val duration = 1f

    init {
        setFontScale(0.5f)

        setPosition(3f, 100f)

        addAction(Actions.sequence(Actions.moveTo(x, y + 60, duration + 0.5f), Actions.removeActor()))
        addAction(Actions.sequence(Actions.delay(duration), Actions.fadeOut(0.5f)))
    }

    override fun act(delta: Float) {
        super.act(delta)

        //lifeTime.update(delta)
    }

    override fun draw(batch: Batch, parentAlpha: Float) {
        super.draw(batch, parentAlpha)


    }
}