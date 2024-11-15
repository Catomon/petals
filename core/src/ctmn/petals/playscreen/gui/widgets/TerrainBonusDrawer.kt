package ctmn.petals.playscreen.gui.widgets

import com.badlogic.gdx.Input
import com.badlogic.gdx.graphics.Color
import com.badlogic.gdx.graphics.g2d.Batch
import com.badlogic.gdx.graphics.g2d.Sprite
import com.badlogic.gdx.scenes.scene2d.Actor
import com.badlogic.gdx.scenes.scene2d.InputEvent
import com.badlogic.gdx.scenes.scene2d.InputListener
import com.badlogic.gdx.scenes.scene2d.Stage
import com.kotcrab.vis.ui.widget.VisLabel
import ctmn.petals.Const
import ctmn.petals.GamePref
import ctmn.petals.playscreen.events.UnitSelectedEvent
import ctmn.petals.playscreen.gui.PlayGUIStage
import ctmn.petals.playscreen.playStage
import ctmn.petals.playscreen.playStageOrNull
import ctmn.petals.playstage.PlayStage
import ctmn.petals.playstage.getTileMovementCostMatrix
import ctmn.petals.playstage.getTiles
import ctmn.petals.playstage.isInRange
import ctmn.petals.tile.isOccupied
import ctmn.petals.unit.*
import ctmn.petals.utils.centerX
import ctmn.petals.utils.centerY
import ctmn.petals.utils.unTiled

class TerrainBonusDrawer(val guiStage: PlayGUIStage) : Actor() {

    var unitActor: UnitActor? = null

    val label = VisLabel("costLabel", "default")

    var isDisabled = !GamePref.showTerrainBonus

    init {
        isVisible = !isDisabled

        label.isVisible = false
        label.setFontScale(0.25f)
        //label.color = Color.BLUE

        guiStage.addListener {
            if (it is UnitSelectedEvent) {
                unitActor = it.unit
            }

            false
        }

        guiStage.addListener(object : InputListener() {
            override fun keyUp(event: InputEvent?, keycode: Int): Boolean {
                if (keycode == Input.Keys.B) {
                    isVisible = !isVisible
                }

                return false
            }
        })
    }

    override fun draw(batch: Batch, parentAlpha: Float) {
        if (isDisabled) return

        val unitActor = unitActor ?: return

        if (guiStage.mapClickListener != guiStage.unitSelectedCL) return

        if (unitActor.actionPoints < Const.ACTION_POINTS_MOVE_MIN) return

        val terrainProps = unitActor.cTerrainProps ?: return

        playStageOrNull ?: return

        playStage.getTiles().forEach {
            val tileTerPr = terrainProps.get(it.terrain)
            if ((tileTerPr.attackBonus != 0 || tileTerPr.defenseBonus != 0)
                && isInRange(it.tiledX, it.tiledY, unitActor.movingRange, unitActor.tiledX, unitActor.tiledY)
                && !it.isOccupied
            ) {
                label.setText("${tileTerPr.attackBonus} / ${tileTerPr.defenseBonus}")
                label.pack()

                label.setText("/")
                label.pack()
                val slashW = label.width
                val slashH = label.height
                label.setPosition(it.centerX - label.width / 2, it.centerY - label.height / 2)
                label.draw(batch, parentAlpha)

                label.setText("${tileTerPr.attackBonus}")
                label.pack()
                label.setPosition(it.centerX - label.width - slashW, it.centerY - label.height / 2 + slashH / 2)
                label.draw(batch, parentAlpha)

                label.setText("${tileTerPr.defenseBonus}")
                label.pack()
                label.setPosition(it.centerX + slashW / 2, it.centerY - label.height / 2 - slashH / 2)
                label.draw(batch, parentAlpha)
            }
        }
    }

    override fun setStage(stage: Stage?) {
        super.setStage(stage)

        if (stage != null && stage !is PlayStage) throw IllegalArgumentException("This actor can be added only to PlayStage")
    }

    override fun setVisible(visible: Boolean) {
        super.setVisible(visible)

        label.isVisible = visible
    }
}