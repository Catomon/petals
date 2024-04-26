package ctmn.petals.playscreen.gui.widgets

import ctmn.petals.playscreen.gui.PlayGUIStage
import ctmn.petals.unit.*
import ctmn.petals.unit.component.AnimationViewComponent
import com.badlogic.gdx.scenes.scene2d.Actor
import ctmn.petals.effects.Animations
import ctmn.petals.playscreen.events.ActionCompletedEvent
import ctmn.petals.playscreen.events.CommandExecutedEvent
import ctmn.petals.playstage.*
import ctmn.petals.unit.UnitActor
import ctmn.petals.utils.*

class AnimationsUpdater(val guiStage: PlayGUIStage) : Actor() {

    private val playStage = guiStage.playStage

    init {
        guiStage.addListener {
            if (it is CommandExecutedEvent || it is ActionCompletedEvent) {
                updateUnitFlip()
            }

            false
        }
    }

    override fun act(delta: Float) {
        super.act(delta)

        Animations.update(delta)

        updateUnitViewComponent()
    }

    private fun updateUnitViewComponent() {
        //anime if non-default animation
        for (unit in guiStage.playStage.getUnits())
            if (unit.viewComponent is AnimationViewComponent
                && (unit.viewComponent as AnimationViewComponent).animation != unit.defaultAnimation)
                (unit.viewComponent as AnimationViewComponent).isAnimate = true

        //anime enemy units at their turn if they have action points
        if (guiStage.theirTurn == guiStage.currentState) {
            for (unit in guiStage.playStage.getUnitsOfEnemyOf(guiStage.localPlayer))
                if (unit.actionPoints > 0) {
                    if (unit.viewComponent is AnimationViewComponent)
                        (unit.viewComponent as AnimationViewComponent).isAnimate = true
                }
        }

        //animate units with the same leaderID as selected unit, if AP > 0
        if (guiStage.selectedUnit != null) {
            if (guiStage.selectedUnit!!.viewComponent is AnimationViewComponent) {
                (guiStage.selectedUnit!!.viewComponent as AnimationViewComponent).isAnimate = true
            }

            for (unit in guiStage.playStage.getUnitsForLeader(guiStage.selectedUnit!!.leaderID)) {
                if (unit.actionPoints > 0)
                    if (unit.viewComponent is AnimationViewComponent) {
                        (unit.viewComponent as AnimationViewComponent).isAnimate = true
                    }
            }

            return
        }

        //get focused unit, return if null
        val focusedUnit = guiStage.playStage.getUnit(
            guiStage.tileSelectionDrawer.hoveringSprite.centerX().tiled(),
            guiStage.tileSelectionDrawer.hoveringSprite.centerY().tiled()) ?: return

        //animate units with the same leaderID as focused unit, if AP > 0
        for (unit in guiStage.playStage.getUnitsForLeader(focusedUnit.cLeader?.leaderID ?: focusedUnit.followerID)) {
            if (unit.actionPoints > 0) {
                if (unit.viewComponent is AnimationViewComponent)
                    (unit.viewComponent as AnimationViewComponent).isAnimate = true
            }
        }
    }

    private fun updateUnitFlip() {
        for (unit in playStage.getUnits()) {
            if (!unit.viewComponent.flipToEnemy) continue

            unit.viewComponent.flipX = false
            var flipX = false

            if (playStage.getUnit(unit.tiledX, unit.tiledY + 1)?.isAlly(unit) == false
                && playStage.getUnit(unit.tiledX - 1, unit.tiledY)?.isAlly(unit) == false) flipX = true

            if (playStage.getUnit(unit.tiledX, unit.tiledY - 1)?.isAlly(unit) == false
                && playStage.getUnit(unit.tiledX - 1, unit.tiledY)?.isAlly(unit) == false) flipX = true

            var closestUnit: UnitActor? = null

            for (u in playStage.getUnits()) {
                if (u.isAlly(unit)) continue

                if (closestUnit == null) {
                    closestUnit = u
                    continue
                }

                if (tiledDst(u.tiledX, u.tiledY, unit.tiledX, unit.tiledY) < tiledDst(closestUnit.tiledX, closestUnit.tiledY, unit.tiledX, unit.tiledY))
                    closestUnit = u
            }

            if (closestUnit != null) {
                if (closestUnit.tiledX < unit.tiledX)
                    flipX = true
            }

            unit.viewComponent.flipX = flipX
        }
    }
}
