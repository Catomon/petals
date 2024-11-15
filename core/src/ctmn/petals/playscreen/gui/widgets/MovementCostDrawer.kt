package ctmn.petals.playscreen.gui.widgets

import ctmn.petals.Const
import ctmn.petals.playscreen.gui.PlayGUIStage
import ctmn.petals.playscreen.events.UnitSelectedEvent
import ctmn.petals.unit.UnitActor
import ctmn.petals.utils.unTiled
import com.badlogic.gdx.Input
import com.badlogic.gdx.graphics.Color
import com.badlogic.gdx.graphics.g2d.Batch
import com.badlogic.gdx.graphics.g2d.Sprite
import com.badlogic.gdx.scenes.scene2d.Actor
import com.badlogic.gdx.scenes.scene2d.InputEvent
import com.badlogic.gdx.scenes.scene2d.InputListener
import com.badlogic.gdx.scenes.scene2d.Stage
import com.kotcrab.vis.ui.widget.VisLabel
import ctmn.petals.playstage.PlayStage
import ctmn.petals.playstage.getTileMovementCostMatrix

class MovementCostDrawer(val guiStage: PlayGUIStage) : Actor() {

    var unitActor: UnitActor? = null

    val sprite = Sprite(guiStage.assets.findAtlasRegion("gui/white"))

    val costLabel = VisLabel("costLabel", "default")

    init {
        isVisible = false

        sprite.setSize(Const.TILE_SIZE.toFloat(), Const.TILE_SIZE.toFloat())

        costLabel.isVisible = false
        costLabel.setFontScale(0.25f)
        costLabel.color = Color.BLUE

        guiStage.addListener {
            if (it is UnitSelectedEvent) {
                unitActor = it.unit
            }

            false
        }

        guiStage.addListener(object : InputListener() {
            override fun keyUp(event: InputEvent?, keycode: Int): Boolean {
                if (keycode == Input.Keys.M) {
                    isVisible = !isVisible
                }

                return false
            }
        })
    }

    override fun draw(batch: Batch, parentAlpha: Float) {
        super.draw(batch, parentAlpha)

        if (unitActor == null) return

        val moveCostMap = guiStage.playStage.getTileMovementCostMatrix(unitActor!!)

        for (x in moveCostMap.indices) {
            for (y in moveCostMap[x].indices) {

                val tile = guiStage.playStage.getTile(x, y)
                if (tile != null) {
                    costLabel.setText(moveCostMap[x][y])

                } else costLabel.setText("null_tile")

                costLabel.setPosition(x.unTiled() + 6, y.unTiled() - 6f)
                costLabel.draw(batch, parentAlpha)
            }
        }
    }

    override fun setStage(stage: Stage?) {
        super.setStage(stage)

        if (stage != null && stage !is PlayStage) throw IllegalArgumentException("This actor can be added only to PlayStage")

        if (stage == null)
            costLabel.remove()
        else guiStage.playStage.addActor(costLabel)
    }

    override fun setVisible(visible: Boolean) {
        super.setVisible(visible)

        costLabel.isVisible = visible
    }
}