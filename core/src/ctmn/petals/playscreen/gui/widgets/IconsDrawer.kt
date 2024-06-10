package ctmn.petals.playscreen.gui.widgets

import com.badlogic.gdx.graphics.g2d.Batch
import com.badlogic.gdx.scenes.scene2d.Actor
import ctmn.petals.Const.TILE_SIZE
import ctmn.petals.assets
import ctmn.petals.playscreen.gui.PlayGUIStage
import ctmn.petals.playscreen.playStage
import ctmn.petals.playstage.getUnits
import ctmn.petals.resizeFromPui
import ctmn.petals.tile.components.BaseBuildingComponent
import ctmn.petals.tile.components.CapturingComponent
import ctmn.petals.tile.components.DestroyingComponent
import ctmn.petals.unit.tiledX
import ctmn.petals.unit.tiledY
import ctmn.petals.utils.AnimatedSprite

class IconsDrawer(val gui: PlayGUIStage) : Actor() {

    private val capturingIc = AnimatedSprite(assets.findAtlasRegions("gui/icons/capturing"), 0.7f).resizeFromPui()
    private val baseBuildingIc = AnimatedSprite(assets.findAtlasRegions("gui/icons/building"), 0.7f).resizeFromPui()
    private val destroyingIc = AnimatedSprite(assets.findAtlasRegions("gui/icons/destroying"), 0.7f).resizeFromPui()

    private val halfTileSize = TILE_SIZE / 2

    override fun act(delta: Float) {
        super.act(delta)

        capturingIc.update(delta)
        baseBuildingIc.update(delta)
        destroyingIc.update(delta)
    }

    override fun draw(batch: Batch?, parentAlpha: Float) {
        super.draw(batch, parentAlpha)

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
            }
        }
    }
}