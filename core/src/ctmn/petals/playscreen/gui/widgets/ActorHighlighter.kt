package ctmn.petals.playscreen.gui.widgets

import com.badlogic.gdx.graphics.g2d.Batch
import com.badlogic.gdx.math.Rectangle
import com.badlogic.gdx.math.Vector2
import com.badlogic.gdx.scenes.scene2d.Actor
import com.badlogic.gdx.scenes.scene2d.InputEvent
import com.kotcrab.vis.ui.widget.VisImage
import ctmn.petals.playscreen.gui.PlayGUIStage
import ctmn.petals.utils.setPosByCenter

class ActorHighlighter(private val guiStage: PlayGUIStage) : Actor() {

    val targetNames: MutableList<String> = ArrayList()
    val targetActors: MutableList<Actor> = ArrayList()
    private val highlight = VisImage("highlight_circle")
    private var tmpVec = Vector2()
    private var tmpRect = Rectangle(0f, 0f, 80f, 80f)

    init {
        highlight.setSize(120f, 120f)

        guiStage.addCaptureListener {
            if (it !is InputEvent) return@addCaptureListener false
            if (it.type != InputEvent.Type.touchDown) return@addCaptureListener false

            targetNames.removeIf { targetName ->
                val targetActor = guiStage.root.findActor<Actor>(targetName)
                targetActor.localToStageCoordinates(tmpVec.set(targetActor.width / 2, targetActor.height / 2))
                tmpRect.setPosition(tmpVec.x - tmpRect.width / 2, tmpVec.y - tmpRect.height / 2)
                tmpRect.contains(it.stageX, it.stageY)
            }

            targetActors.removeIf { targetActor ->
                targetActor.localToStageCoordinates(tmpVec.set(targetActor.width / 2, targetActor.height / 2))
                tmpRect.setPosition(tmpVec.x - tmpRect.width / 2, tmpVec.y - tmpRect.height / 2)
                tmpRect.contains(it.stageX, it.stageY)
            }

            false
        }
    }

    private var dir = false

    override fun act(delta: Float) {
        super.act(delta)

        toFront()

        if (dir) {
            highlight.width -= 200 * delta
            highlight.height = highlight.width

            if (highlight.width < 80) {
                dir = false
            }
        } else {
            highlight.width += 200 * delta
            highlight.height = highlight.width

            if (highlight.width > 120) {
                dir = true
            }
        }
    }

    override fun draw(batch: Batch?, parentAlpha: Float) {
        for (targetActor in targetActors + targetNames.mapNotNull { guiStage.root.findActor(it) }) {
            if (!targetActor.isVisible) return
            targetActor.localToStageCoordinates(tmpVec.set(targetActor.width / 2, targetActor.height / 2))
            highlight.setPosByCenter(tmpVec.x, tmpVec.y)
            highlight.draw(batch, parentAlpha)
        }
    }
}