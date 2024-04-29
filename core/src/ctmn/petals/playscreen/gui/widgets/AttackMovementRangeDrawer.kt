package ctmn.petals.playscreen.gui.widgets

import com.badlogic.gdx.graphics.Color
import com.badlogic.gdx.scenes.scene2d.Group
import ctmn.petals.GamePref
import ctmn.petals.playscreen.commands.AttackCommand
import ctmn.petals.playscreen.events.CommandAddedEvent
import ctmn.petals.playscreen.events.UnitMovedEvent
import ctmn.petals.playscreen.events.UnitSelectedEvent
import ctmn.petals.playstage.getMovementGrid
import ctmn.petals.playscreen.gui.PlayGUIStage
import ctmn.petals.playscreen.playStage
import ctmn.petals.playscreen.seqactions.AttackAction
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

                is CommandAddedEvent -> {
                    when (it.command) {
                        is AttackCommand -> {
                            unit = playStage.root.findActor(it.command.attackerUnitId) ?: return@addListener false

                            isVisible = false

                            updateBorders()
                        }

//                        is BuyUnitCommand -> {
//
//                        }

                        else -> {
                            unit = null
                            isVisible = false
                        }
                    }
                }
            }

            false
        }
    }

    private fun updateBorders() {
        if (unit?.isPlayerUnit(guiStage.localPlayer) == false && GamePref.showAiGui != true
            && guiStage.playScreen.botManager.isBotPlayer(guiStage.playScreen.turnManager.currentPlayer)) {
            this.unit = null
            isVisible = false
        }

        val unit = unit
        if (unit != null) {
            moveRangeBorder.makeForMatrix(guiStage.playStage.getMovementGrid(unit, true), guiStage.playStage)
            attackRangeBorder.makeForMatrix(
                guiStage.playStage.getMovementGrid(
                    unit.attackRange,
                    unit.tiledX,
                    unit.tiledY,
                    TerrainPropsPack.clear
                ), guiStage.playStage
            )
            if (unit.cAttack!!.attackRangeBlocked > 0)
                minAttackRangeBorder.makeForMatrix(
                    guiStage.playStage.getMovementGrid(
                        unit.cAttack!!.attackRangeBlocked,
                        unit.tiledX,
                        unit.tiledY,
                        TerrainPropsPack.clear
                    ), guiStage.playStage
                )
            else
                minAttackRangeBorder.isVisible = false
        }
    }

    override fun act(delta: Float) {
        super.act(delta)

        val unit = unit
        isVisible = unit != null
                && guiStage.playScreen.fogOfWarManager.isVisible(unit.tiledX, unit.tiledY)
                //&& guiStage.mapClickListener == guiStage.seeInfoCL || guiStage.mapClickListener == guiStage.unitSelectedCL
                && !guiStage.playScreen.actionManager.hasActions || guiStage.playScreen.actionManager.currentAction is AttackAction
    }

    override fun setVisible(visible: Boolean) {
        super.setVisible(visible)

        moveRangeBorder.show(isVisible && unit?.canMove() == true && unit!!.isPlayerUnit(guiStage.localPlayer))

        if (unit != null && !unit!!.canMove() && !unit!!.isPlayerUnit(guiStage.localPlayer))
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