package ctmn.petals.playscreen.gui.widgets

import ctmn.petals.Const
import ctmn.petals.Const.TILE_SIZE
import ctmn.petals.playscreen.gui.PlayGUIStage
import ctmn.petals.playscreen.events.UnitSelectedEvent
import ctmn.petals.unit.*
import ctmn.petals.utils.setPositionByCenter
import ctmn.petals.utils.unTiled
import com.badlogic.gdx.graphics.g2d.Batch
import com.badlogic.gdx.graphics.g2d.Sprite
import com.badlogic.gdx.scenes.scene2d.Actor
import com.badlogic.gdx.scenes.scene2d.actions.Actions
import ctmn.petals.newPlaySprite
import ctmn.petals.playstage.getUnits

class AttackIconsDrawer(private val guiStage: PlayGUIStage) : Actor() {

    private val attackSprite: Sprite = newPlaySprite(guiStage.assets.textureAtlas.findRegion("gui/attack_icon"))

    init {
        attackSprite.setSize(Const.TILE_SIZE.toFloat() / 2, Const.TILE_SIZE.toFloat() / 2)

        guiStage.addListener {
            if (it is UnitSelectedEvent) {
                if (it.unit != null) {
                    actions.clear()
                    setSize(8f, 8f)
                    addAction(Actions.sizeTo(64f, 64f, 0.15f))
                }
            }

            false
        }
    }

    override fun draw(batch: Batch, parentAlpha: Float) {
        if (!guiStage.isUnitSelected)
            return

        if (guiStage.selectedUnit?.canAttack() == true) {
            for (unit in guiStage.playStage.getUnits()) {
                if (unit.isAlly(guiStage.selectedUnit!!) || !unit.isVisible)
                    continue

                if (guiStage.selectedUnit!!.inAttackRange(unit.tiledX, unit.tiledY)) {
                    if (unit.tiledX != guiStage.selectedUnit!!.tiledX || unit.tiledY != guiStage.selectedUnit!!.tiledY) {
                        attackSprite.setPositionByCenter(unit.tiledX.unTiled() + TILE_SIZE / 2, unit.tiledY.unTiled() + TILE_SIZE / 2)
                        attackSprite.draw(batch)
                    }
                }
            }
        }
    }

    override fun sizeChanged() {
        super.sizeChanged()

        attackSprite.setSize(width, height)
    }
}
