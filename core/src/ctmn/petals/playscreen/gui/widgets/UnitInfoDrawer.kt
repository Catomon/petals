package ctmn.petals.playscreen.gui.widgets

import com.badlogic.gdx.graphics.Color
import ctmn.petals.playstage.PlayStage
import ctmn.petals.playstage.getUnits
import ctmn.petals.playscreen.gui.PlayGUIStage
import ctmn.petals.unit.*
import ctmn.petals.utils.RegionAnimation
import com.badlogic.gdx.graphics.g2d.Batch
import com.badlogic.gdx.scenes.scene2d.Actor
import com.badlogic.gdx.scenes.scene2d.Stage
import com.kotcrab.vis.ui.widget.VisLabel
import ctmn.petals.newPlayPuiSprite
import ctmn.petals.newPlaySprite
import ctmn.petals.playstage.getCapturablesOf
import ctmn.petals.tile.components.ActionCooldown
import ctmn.petals.tile.isBase
import ctmn.petals.unit.UnitActor
import ctmn.petals.unit.component.BarrierComponent
import ctmn.petals.utils.centerX
import ctmn.petals.utils.centerY
import ctmn.petals.utils.setPositionByCenter

//todo move this class to pui stage
class UnitInfoDrawer(val guiStage: PlayGUIStage) : Actor() {

    var drawHealthByText = false //if true draw health as text else as heart icon
    var drawHealthLabel = false
    var hideIfFullHealth = true

    //health heart
    private val heartRegions = guiStage.assets.atlases.findRegions("gui/unit_heart")
    private val heartGlassRegions = guiStage.assets.atlases.findRegions("gui/unit_heart_glass")

    private val heartSprite = newPlayPuiSprite(heartRegions.first())
    private val heartGlassSprite = newPlayPuiSprite(heartGlassRegions.first())

    private val greenFrames = guiStage.assets.atlases.findRegions("gui/action_points_available")
    private val redFrames = guiStage.assets.atlases.findRegions("gui/action_points_available_red")
    private val blueFrames = guiStage.assets.atlases.findRegions("gui/action_points_available_blue")

    private val actionPointsAnimation = RegionAnimation(0.6f, greenFrames)
    private val actionPointsSprite = newPlayPuiSprite(actionPointsAnimation.currentFrame)

    private val heartGlassAni = RegionAnimation(0.05f, heartGlassRegions)
    private val heartGlassAniDelay = 1.5f
    private var heartGlassAniCurDelay = heartGlassAniDelay

    private val healthLabel = VisLabel("$this.healthLabel", "font_5").also {
        it.isVisible = false
        it.setFontScale(0.25f)

        //see overridden setStage()
        //guiStage.playStage.addActor(it)
    }

    override fun act(delta: Float) {
        super.act(delta)

        actionPointsAnimation.update(delta)

        heartGlassAni.update(delta)

        if (heartGlassAni.stateTime >= heartGlassAni.animationDuration) {
            heartGlassSprite.setRegion(heartGlassAni.keyFrames.first())
            heartGlassAniCurDelay -= delta
            if (heartGlassAniCurDelay <= 0) {
                heartGlassAniCurDelay = heartGlassAniDelay
                heartGlassAni.stateTime = 0f
            }
        } else heartGlassSprite.setRegion(heartGlassAni.currentFrame)
    }

    override fun draw(batch: Batch, parentAlpha: Float) {
        super.draw(batch, parentAlpha)

        if (guiStage.playScreen.turnManager.currentPlayer == guiStage.localPlayer) {
            actionPointsAnimation.setFrames(greenFrames)
            actionPointsSprite.setRegion(actionPointsAnimation.currentFrame)
            for (capturable in guiStage.playStage.getCapturablesOf(guiStage.localPlayer)) {
                if (capturable.isBase && !capturable.has(ActionCooldown::class.java)) {
                    actionPointsSprite.setPosition(capturable.x - 2f, capturable.y + 4f)
                    actionPointsSprite.draw(batch)
                }
            }
        }

        for (unit in guiStage.playStage.getUnits()) {
            if (!unit.isVisible)
                continue

            drawForUnit(unit, batch)

            if (unit.isPlayerUnit(guiStage.playScreen.turnManager.currentPlayer)) {
                actionPointsSprite.setPosition(unit.x - 2f, unit.y - 2f)

                if (unit.isPlayerUnit(guiStage.localPlayer))
                    actionPointsAnimation.setFrames(greenFrames)
                else
                    if (guiStage.localPlayer.isAlly(unit.teamId))
                        actionPointsAnimation.setFrames(blueFrames)
                    else
                        actionPointsAnimation.setFrames(redFrames)

                when {
                    unit.canMove() -> {
                        actionPointsSprite.setRegion(actionPointsAnimation.currentFrame)
                        actionPointsSprite.draw(batch)
                    }
                    unit.canAttack() -> {
                        actionPointsSprite.setRegion(actionPointsAnimation.keyFrames[0])
                        actionPointsSprite.draw(batch)
                    }
                }
            }
        }
    }

    private fun drawForUnit(unit: UnitActor, batch: Batch) {
        if (hideIfFullHealth && unit.health >= unit.unitComponent.baseHealth)
            return

        if (drawHealthByText && drawHealthLabel) {
            var h = unit.health / 10
            if (h < 10)
                h += 1

            if (unit.get(BarrierComponent::class.java)?.let {
                h += it.amount / 10
                it
                } != null) {
                healthLabel.color = Color.ORANGE
            } else
                healthLabel.color = Color.WHITE


            healthLabel.setText(h)
            healthLabel.setPosition(unit.x - 2f, unit.y - 2f)

            healthLabel.draw(batch, unit.color.a)
        }

        if (drawHealthByText)
            return

        heartSprite.setPositionByCenter(unit.centerX, unit.centerY)
        heartGlassSprite.setPositionByCenter(unit.centerX, unit.centerY)

        val regIndex = when {
            unit.health >= unit.unitComponent.baseHealth -> 0
            unit.health >= unit.unitComponent.baseHealth * 0.9 -> 0
            unit.health >= unit.unitComponent.baseHealth * 0.7 -> 1
            unit.health >= unit.unitComponent.baseHealth * 0.5 -> 2
            unit.health >= unit.unitComponent.baseHealth * 0.25 -> 3
            unit.health >= unit.unitComponent.baseHealth * 0.15 -> 4
            else -> 5
        }

        heartSprite.setRegion(heartRegions[regIndex])

        heartSprite.draw(batch)
        heartGlassSprite.draw(batch)
    }

    override fun setStage(stage: Stage?) {
        super.setStage(stage)

        if (stage != null && stage !is PlayStage) throw IllegalArgumentException("This actor can be added only to PlayStage")

        if (stage == null)
            healthLabel.remove()
        else guiStage.playStage.addActor(healthLabel)
    }
}
