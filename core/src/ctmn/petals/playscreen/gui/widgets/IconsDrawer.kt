package ctmn.petals.playscreen.gui.widgets

import com.badlogic.gdx.graphics.g2d.Batch
import com.badlogic.gdx.scenes.scene2d.Actor
import ctmn.petals.Const.TILE_SIZE
import ctmn.petals.assets
import ctmn.petals.newPlayPuiSprite
import ctmn.petals.playscreen.gui.PlayGUIStage
import ctmn.petals.playscreen.playStage
import ctmn.petals.playstage.getUnits
import ctmn.petals.resizeFromPui
import ctmn.petals.tile.components.BaseBuildingComponent
import ctmn.petals.tile.components.CapturingComponent
import ctmn.petals.tile.components.DestroyingComponent
import ctmn.petals.unit.actors.FairyHealer
import ctmn.petals.unit.actors.FairyShield
import ctmn.petals.unit.component.BonusFieldComponent
import ctmn.petals.unit.component.ReloadingComponent
import ctmn.petals.unit.tiledX
import ctmn.petals.unit.tiledY
import ctmn.petals.utils.*

class IconsDrawer(val gui: PlayGUIStage) : Actor() {

    private val capturingIc = AnimatedSprite(assets.findAtlasRegions("gui/icons/capturing"), 0.7f).resizeFromPui()
    private val baseBuildingIc = AnimatedSprite(assets.findAtlasRegions("gui/icons/building"), 0.7f).resizeFromPui()
    private val destroyingIc = AnimatedSprite(assets.findAtlasRegions("gui/icons/destroying"), 0.7f).resizeFromPui()
    private val reloadingIc = AnimatedSprite(assets.findAtlasRegions("gui/icons/reloading"), 0.7f).resizeFromPui()
    private val healingRangeIc = newPlayPuiSprite(assets.findAtlasRegion("gui/heal_area_heart")).apply {
        color.a = 0.4f
    }
    private val defenseRangeIc = newPlayPuiSprite(assets.findAtlasRegion("gui/defense_area_shield")).apply {
        color.a = 0.4f
    }

    private val halfTileSize = TILE_SIZE / 2

    override fun act(delta: Float) {
        super.act(delta)

        capturingIc.update(delta)
        baseBuildingIc.update(delta)
        destroyingIc.update(delta)
        reloadingIc.update(delta)
    }

    override fun draw(batch: Batch?, parentAlpha: Float) {
        super.draw(batch, parentAlpha)

        gui.selectedUnit?.let { unit ->
            when (unit) {
                is FairyHealer -> {
                    gui.playStage.getTilesInRange(
                        unit.tiledX,
                        unit.tiledY,
                        unit.get(BonusFieldComponent::class.java)?.range ?: 0,
                        true,
                    ).forEach { tile ->
                        healingRangeIc.setPositionByCenter(tile.centerX, tile.centerY)
                        healingRangeIc.draw(batch, parentAlpha)
                    }
                }

                is FairyShield -> {
                    gui.playStage.getTilesInRange(
                        unit.tiledX,
                        unit.tiledY,
                        unit.get(BonusFieldComponent::class.java)?.range ?: 0,
                        true,
                    ).forEach { tile ->
                        defenseRangeIc.setPositionByCenter(tile.centerX, tile.centerY)
                        defenseRangeIc.draw(batch, parentAlpha)
                    }
                }
            }
        }

        for (unit in playStage.getUnits()) {
            if (gui.playScreen.fogOfWarManager.isVisible(unit.tiledX, unit.tiledY)
            ) {
                val tile = playStage.getTile(unit.tiledX, unit.tiledY) ?: break
                if (tile.has(CapturingComponent::class.java)) {
                    capturingIc.setPosition(unit.x - halfTileSize, unit.y - halfTileSize)
                    capturingIc.draw(batch, parentAlpha)
                }
                if (tile.has(BaseBuildingComponent::class.java)) {
                    baseBuildingIc.setPosition(unit.x - halfTileSize, unit.y - halfTileSize)
                    baseBuildingIc.draw(batch, parentAlpha)
                }
                if (tile.has(DestroyingComponent::class.java)) {
                    destroyingIc.setPosition(unit.x - halfTileSize, unit.y - halfTileSize)
                    destroyingIc.draw(batch, parentAlpha)
                }
                if ((unit.get(ReloadingComponent::class.java)?.currentTurns ?: 0) > 0) {
                    reloadingIc.setPosition(unit.x - halfTileSize, unit.y - halfTileSize)
                    reloadingIc.draw(batch, parentAlpha)
                }
            }
        }
    }
}