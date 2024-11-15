package ctmn.petals.effects

import ctmn.petals.unit.UnitActor
import com.badlogic.gdx.graphics.Color
import com.badlogic.gdx.graphics.g2d.Batch
import com.badlogic.gdx.scenes.scene2d.actions.Actions
import com.kotcrab.vis.ui.widget.VisLabel
import ctmn.petals.Const.IS_ROUND_HEALTH_CHANGE_LABEL

class HealthChangeEffect(unit: UnitActor, changeVal: Int)
    : VisLabel((if (IS_ROUND_HEALTH_CHANGE_LABEL) changeVal / 10 else changeVal).toString(), "default") {

    //private var lifeTime = LifeTime(1f) { remove() }

    private val duration = 1f

    init {
        color = if (changeVal < 0) Color.RED else Color.GREEN

        setFontScale(0.33f)

        setPosition(unit.x + 2f, unit.y - 2f) //also used at [UnitInfoDrawer.kt]

        addAction(Actions.sequence(Actions.moveTo(x, y + 6, duration), Actions.removeActor()))
    }

    override fun act(delta: Float) {
        super.act(delta)

        //lifeTime.update(delta)
    }

    override fun draw(batch: Batch, parentAlpha: Float) {
        super.draw(batch, parentAlpha)


    }
}
