package ctmn.petals.playstage

import com.badlogic.gdx.graphics.g2d.Batch
import com.badlogic.gdx.graphics.g2d.TextureRegion
import com.badlogic.gdx.utils.Array
import ctmn.petals.Const
import ctmn.petals.assets
import ctmn.petals.newPlaySprite

class Border(val playStage: PlayStage) {

    private val borders = Array<BorderTile>()
    private val borderSprite = newPlaySprite(assets.atlases.getRegion("map_frame"))

    fun draw(batch: Batch) {
        for (border in borders) {
            borderSprite.setRegion(border.texture)
            borderSprite.setPosition(border.x, border.y)
            borderSprite.draw(batch)
        }
    }

    fun make() {
        with(playStage) {

            borders.clear()

            val closestZeroTile =
                getAllTiles().minByOrNull { it.tiledX + it.tiledY } ?: throw IllegalStateException("No tiles")
            borders.add(BorderTile("null", closestZeroTile.tiledX.toFloat(), closestZeroTile.tiledY.toFloat()))

            val newBorders = Array<BorderTile>()

            fun checkAndCreate(tiledX: Int, tiledY: Int) {
                if (getTile(tiledX, tiledY) != null) return

                for (x in (tiledX - 1)..(tiledX + 1)) {
                    for (y in (tiledY - 1)..(tiledY + 1)) {
                        if (borders.firstOrNull { it.x == tiledX.toFloat() && it.y == tiledY.toFloat() } == null
                            && newBorders.firstOrNull { it.x == tiledX.toFloat() && it.y == tiledY.toFloat() } == null
                            && getAllTiles().firstOrNull { it.tiledX == x && it.tiledY == y } != null)
                            newBorders.add(BorderTile("null", tiledX.toFloat(), tiledY.toFloat()))
                    }
                }
            }

            while (borders.firstOrNull { it.id == "null" } != null) {
                for (border in Array.ArrayIterator(borders)) {
                    if (border.id != "null") continue

                    val currentX = border.x.toInt()
                    val currentY = border.y.toInt()

                    checkAndCreate(currentX + 1, currentY)
                    checkAndCreate(currentX - 1, currentY)
                    checkAndCreate(currentX, currentY + 1)
                    checkAndCreate(currentX, currentY - 1)

                    val left = if (getTile(border.x.toInt() - 1, border.y.toInt()) != null) "l" else ""
                    val right = if (getTile(border.x.toInt() + 1, border.y.toInt()) != null) "r" else ""
                    val top = if (getTile(border.x.toInt(), border.y.toInt() + 1) != null) "t" else ""
                    val bottom = if (getTile(border.x.toInt(), border.y.toInt() - 1) != null) "b" else ""

                    border.id = "$left$right$top$bottom"
                }

                borders.addAll(newBorders)
                newBorders.clear()
            }

            for (border in borders) {
                border.x *= Const.TILE_SIZE
                border.y *= Const.TILE_SIZE
                border.x -= (borderSprite.width - Const.TILE_SIZE) / 2
                border.y -= (borderSprite.height - Const.TILE_SIZE) / 2

                if (border.id != "") border.id = "_${border.id}"

                border.texture = assets.atlases.findRegion("map_frame${border.id}")
                    ?: throw IllegalArgumentException("no such texture: map_frame${border.id}")
            }

            borders.removeValue(borders.first(), false)
        }
    }

    private inner class BorderTile(var id: String, var x: Float, var y: Float) {
        var texture: TextureRegion? = null
    }
}