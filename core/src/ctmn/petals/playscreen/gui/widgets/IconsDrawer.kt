package ctmn.petals.playscreen.gui.widgets

import com.badlogic.gdx.graphics.g2d.Batch
import com.badlogic.gdx.scenes.scene2d.Actor
import ctmn.petals.Const.TILE_SIZE
import ctmn.petals.assets
import ctmn.petals.playscreen.gui.PlayGUIStage
import ctmn.petals.playscreen.playStage
import ctmn.petals.playstage.getUnits
import ctmn.petals.resizeFromPui
import ctmn.petals.tile.components.CapturingComponent
import ctmn.petals.unit.tiledX
import ctmn.petals.unit.tiledY
import ctmn.petals.utils.AnimatedSprite

class IconsDrawer(val gui: PlayGUIStage) : Actor() {

    private val capturingIc = AnimatedSprite(assets.findAtlasRegions("gui/icons/capturing"), 0.7f).resizeFromPui()

    private val halfTileSize = TILE_SIZE / 2

    override fun act(delta: Float) {
        super.act(delta)

        capturingIc.update(delta)
    }

    override fun draw(batch: Batch?, parentAlpha: Float) {
        super.draw(batch, parentAlpha)

        for (unit in playStage.getUnits()) {
            if (gui.playScreen.fogOfWarManager.isVisible(unit.tiledX, unit.tiledY) && playStage.getTile(
                    unit.tiledX,
                    unit.tiledY
                )?.has(CapturingComponent::class.java) == true
            ) {
                capturingIc.setPosition(unit.x - halfTileSize, unit.y - halfTileSize)
                capturingIc.draw(batch, parentAlpha)
            }
        }
    }
}