package ctmn.petals.editor.ui

import com.badlogic.gdx.math.Vector2
import com.badlogic.gdx.scenes.scene2d.Actor
import com.badlogic.gdx.scenes.scene2d.InputEvent
import com.badlogic.gdx.scenes.scene2d.InputListener
import com.badlogic.gdx.scenes.scene2d.Stage
import com.kotcrab.vis.ui.widget.VisLabel

fun Stage.addTooltip(target: Actor, text: String) {
    addActor(TooltipLabel(target, text))
}

fun Actor.addTooltip(text: String) {
    check(stage != null)
    stage?.addActor(TooltipLabel(this, text))
}

class TooltipLabel(val target: Actor, text: String) : VisLabel(text) {

    init {
        isVisible = false

        target.addListener(object : InputListener() {
            override fun enter(event: InputEvent?, x: Float, y: Float, pointer: Int, fromActor: Actor?) {
                super.enter(event, x, y, pointer, fromActor)

                isVisible = true
                val pos = target.localToStageCoordinates(Vector2(target.x + target.width, target.height / 2))
                setPosition(pos.x, pos.y - height / 2)
            }

            override fun exit(event: InputEvent?, x: Float, y: Float, pointer: Int, toActor: Actor?) {
                super.exit(event, x, y, pointer, toActor)

                isVisible = false
            }
        })
    }
}