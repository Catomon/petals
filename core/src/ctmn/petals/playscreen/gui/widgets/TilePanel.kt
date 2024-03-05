package ctmn.petals.playscreen.gui.widgets

import com.badlogic.gdx.scenes.scene2d.utils.TextureRegionDrawable
import com.kotcrab.vis.ui.VisUI
import com.kotcrab.vis.ui.widget.VisImage
import com.kotcrab.vis.ui.widget.VisTable
import ctmn.petals.assets
import ctmn.petals.playscreen.gui.PlayGUIStage
import ctmn.petals.unit.cTerrainProps
import ctmn.petals.utils.TilePosition
import ctmn.petals.utils.tiledX
import ctmn.petals.utils.tiledY
import ctmn.petals.widgets.newLabel

class TilePanel(val guiStage: PlayGUIStage) : VisTable() {

    private val tileIconDrawable = TextureRegionDrawable(assets.textureAtlas.findRegion("tiles/grass/grass"))
    private val tileIcon = VisImage(tileIconDrawable)

    private val atkIcon = VisImage(VisUI.getSkin().getDrawable("sword_icon"))
    private val atk = newLabel("ATK: X", "font_5")

    private val defIcon = VisImage(VisUI.getSkin().getDrawable("shield_icon"))
    private val def = newLabel("DEF: X", "font_5")

    private val tilePosition = TilePosition(-1, -1)

    init {
        name = "tile_panel"

        setBackground("unit_panel_background")

        //add(tileIcon)
        add(newLabel("Terrain", "font_5"))
        row()
        add(VisTable().apply {
            add(atkIcon)
            add(atk).width(8f).padRight(2f)
            add(defIcon)
            add(def).width(8f).width(8f).padRight(2f)
        })

        pack()
    }

    override fun act(delta: Float) {
        super.act(delta)

        val hoverX = guiStage.tileSelectionDrawer.tiledX + 1
        val hoverY = guiStage.tileSelectionDrawer.tiledY + 1
        if (hoverX != tilePosition.x || hoverY != tilePosition.y) {
            tilePosition.x = hoverX
            tilePosition.y = hoverY

            val tile = guiStage.playStage.getTile(tilePosition.x, tilePosition.y) ?: return
            val selectedUnit = guiStage.selectedUnit ?: return

            tileIconDrawable.region = tile.sprite
            tileIcon.drawable = tileIconDrawable

            val terrainBuff = selectedUnit.cTerrainProps?.get(tile.terrain) ?: return

            atk.setText(terrainBuff.attackBonus)
            def.setText(terrainBuff.defenseBonus)
        }
    }
}