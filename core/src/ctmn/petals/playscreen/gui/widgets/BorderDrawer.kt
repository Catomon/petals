package ctmn.petals.playscreen.gui.widgets

import ctmn.petals.Const.TILE_SIZE
import ctmn.petals.playscreen.gui.PlayGUIStage
import ctmn.petals.unit.TerrainCosts
import ctmn.petals.unit.sprite
import ctmn.petals.utils.*
import com.badlogic.gdx.graphics.Color
import com.badlogic.gdx.graphics.g2d.Batch
import com.badlogic.gdx.graphics.g2d.Sprite
import com.badlogic.gdx.scenes.scene2d.Actor
import com.badlogic.gdx.scenes.scene2d.actions.Actions
import ctmn.petals.playstage.PlayStage
import ctmn.petals.playstage.getMovementGrid
import ctmn.petals.playstage.getTiles

class BorderDrawer(color: Color?  = null, val guiStage: PlayGUIStage) : Actor() {

    val sprite = Sprite(guiStage.assets.textureAtlas.findRegion("gui/tile_available_mark"))
    private val animation = RegionAnimation(0.1f, guiStage.assets.textureAtlas.findRegions("gui/animated/tile_sel"))
    private val borders = HashMap<Pair<Int, Int>, Border>()

    var borderName = "tile_selection_border"

    var drawOnlyBorder = false

    init {
        if (color != null)
            sprite.color = color
        //sprite.setAlpha(0.75f)

        if (sprite.width < TILE_SIZE)
            sprite.setSize(TILE_SIZE.toFloat(), TILE_SIZE.toFloat())

        setSize(TILE_SIZE.toFloat(), TILE_SIZE.toFloat())

        isVisible = false
    }

    override fun act(delta: Float) {
        super.act(delta)

        animation.update(delta)
    }

    override fun draw(batch: Batch?, parentAlpha: Float) {
        super.draw(batch, parentAlpha)

        for (border in borders.values) {
            //change size if border in on focused tile
            if (actions.isEmpty)
            if (guiStage.tileSelectionDrawer.hoveringSprite.centerX().tiled() == border.tileX
                    && guiStage.tileSelectionDrawer.hoveringSprite.centerY().tiled() == border.tileY) {
                //sprite.setSize(20f, 20f)
            } else {
                sprite.setSize(16f, 16f)
            }

            sprite.setPositionByCenter(border.tileX * TILE_SIZE + TILE_SIZE.toFloat() / 2,
                    border.tileY * TILE_SIZE + TILE_SIZE.toFloat() / 2)

            //skip if border is on selectedUnit
            if (sprite.centerX() ==  guiStage.selectedUnit?.sprite?.centerX() &&
                sprite.centerY() == guiStage.selectedUnit?.sprite?.centerY())
                continue

            //draw background thingy of the border
            sprite.setRegion(animation.currentFrame)
            if (!drawOnlyBorder)
                sprite.draw(batch)

            //draw border
            var marks = ""

            if (borders[border.tileX to border.tileY + 1] != null) marks += "t"
            if (borders[border.tileX to border.tileY - 1] != null) marks += "b"
            if (borders[border.tileX - 1 to border.tileY] != null) marks += "l"
            if (borders[border.tileX + 1 to border.tileY] != null) marks += "r"

            val tileSelectionBorderName = "${borderName}_" + when(marks) {
                "t" -> "p"
                "b" -> "h"
                "l" -> "d"
                "r" -> "b"
                "bl" -> "g"
                "blr" -> "f"
                "br" -> "e"
                "lr" -> "c"
                "tb" -> "l"
                "tbl" -> "k"
                "tblr" -> "j"
                "tbr" -> "i"
                "tl" -> "o"
                "tlr" -> "n"
                "tr" -> "m"
                else -> "a"
            }

            sprite.setRegion(guiStage.assets.textureAtlas.findRegion("gui/$tileSelectionBorderName"))
            sprite.draw(batch)
        }
    }

    fun makeForRange(range: Int, x: Int, y: Int, stage: PlayStage) {
        setPosition(x.unTiled(), y.unTiled())

        makeForMatrix(stage.getMovementGrid(range, x, y, TerrainCosts.ability), stage)
    }

    fun makeForMatrix(matrix: kotlin.Array<IntArray>, stage: PlayStage) {
        borders.clear()

        for (tile in stage.getTiles()) {
            if (matrix[tile.tiledX][tile.tiledY] > 0) {
                //skip if there is an actor on tile
                //if (stage.getUnitActor(tile.tiledX, tile.tiledY) != null)
                //    continue

                borders[tile.tiledX to tile.tiledY] = Border(tile.tiledX, tile.tiledY)
            }
        }
    }

    fun show(visible: Boolean) {
        if (!isVisible && visible)
            show()
        else
            if (isVisible && !visible)
                hide()
    }

    private fun show() {
        if (isVisible || !actions.isEmpty) return

        setSize(TILE_SIZE.toFloat() / 2, TILE_SIZE.toFloat() / 2)
        addAction(Actions.sequence(
            Actions.show(),
            Actions.sizeTo(TILE_SIZE.toFloat(), TILE_SIZE.toFloat(), 0.15f)
        ))
    }

    private fun hide() {
        isVisible = false

//        if (!isVisible || !actions.isEmpty) return
//
//        setSize(TILE_SIZE.toFloat(), TILE_SIZE.toFloat())
//        addAction(Actions.sequence(
//            Actions.sizeTo(TILE_SIZE.toFloat() / 2, TILE_SIZE.toFloat() / 2, 0.1f),
//            Actions.hide()
//        ))
    }

    override fun sizeChanged() {
        super.sizeChanged()

        sprite.setSize(width, height)
    }

    override fun setColor(color: Color?) {
        super.setColor(color)

        sprite.color = color
    }

    override fun setColor(r: Float, g: Float, b: Float, a: Float) {
        super.setColor(r, g, b, a)

        sprite.setColor(r, g, b, a)
    }

    inner class Border(val tileX: Int, val tileY: Int)
}
