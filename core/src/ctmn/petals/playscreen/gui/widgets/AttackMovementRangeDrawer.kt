package ctmn.petals.playscreen.gui.widgets

import com.badlogic.gdx.graphics.Color
import com.badlogic.gdx.scenes.scene2d.Group
import ctmn.petals.GamePref
import ctmn.petals.playscreen.events.UnitMovedEvent
import ctmn.petals.playscreen.events.UnitSelectedEvent
import ctmn.petals.playstage.getMovementGrid
import ctmn.petals.playscreen.gui.PlayGUIStage
import ctmn.petals.unit.*
import ctmn.petals.unit.UnitActor

class AttackMovementRangeDrawer(val guiStage: PlayGUIStage) : Group() {

    private var unit: UnitActor? = null

    private var moveRangeBorder = BorderDrawer(null, guiStage)
    private var minAttackRangeBorder = BorderDrawer(Color.RED, guiStage)
    private var attackRangeBorder = BorderDrawer(Color.RED, guiStage)

    var drawAttackRangeForRangedOnly = true

    init {
        addActor(moveRangeBorder)
        addActor(minAttackRangeBorder)
        addActor(attackRangeBorder)

        attackRangeBorder.sprite.setAlpha(0.5f)
        minAttackRangeBorder.sprite.setAlpha(0.5f)
        attackRangeBorder.drawOnlyBorder = true
        minAttackRangeBorder.drawOnlyBorder = true

        guiStage.addListener {
            when (it) {
                is UnitSelectedEvent -> {
                    unit = it.unit

                    isVisible = false

                    updateBorders()
                }

                is UnitMovedEvent -> {
                    unit = it.unit

                    isVisible = false

                    updateBorders()
                }
            }

            false
        }
    }

    private fun updateBorders() {
        val unit = unit
        if (unit != null) {
            moveRangeBorder.makeForMatrix(guiStage.playStage.getMovementGrid(unit, true), guiStage.playStage)
            attackRangeBorder.makeForMatrix(
                guiStage.playStage.getMovementGrid(
                    unit.attackRange,
                    unit.tiledX,
                    unit.tiledY,
                    TerrainCosts.clear
                ), guiStage.playStage
            )
            if (unit.cAttack!!.attackRangeBlocked > 0)
                minAttackRangeBorder.makeForMatrix(
                    guiStage.playStage.getMovementGrid(
                        unit.attackRange - unit.cAttack!!.attackRangeBlocked,
                        unit.tiledX,
                        unit.tiledY,
                        TerrainCosts.clear
                    ), guiStage.playStage
                )
        }
    }

    override fun act(delta: Float) {
        super.act(delta)

        isVisible =
            unit?.playerId == guiStage.player.id && guiStage.clickStrategy == guiStage.unitSelectedCs && !guiStage.playScreen.actionManager.hasActions
    }

    override fun setVisible(visible: Boolean) {
        super.setVisible(visible)

        moveRangeBorder.show(isVisible && unit?.canMove() == true)

        if (unit != null && !unit!!.canMove())
            moveRangeBorder.isVisible = false

        if (GamePref.drawUnitAttackRange == true) {
            attackRangeBorder.show(isVisible)
            if (unit == null) {
                minAttackRangeBorder.show(isVisible)
            } else {
                if (if (drawAttackRangeForRangedOnly) unit!!.attackRange > 1 else true) {
                    minAttackRangeBorder.show(isVisible && unit!!.cAttack!!.attackRangeBlocked > 0)
                }
            }
        }
    }
}