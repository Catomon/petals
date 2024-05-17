package ctmn.petals.playscreen.gui.widgets

import ctmn.petals.playstage.getMovementGrid
import ctmn.petals.playscreen.gui.PlayGUIStage
import ctmn.petals.tile.isOccupied
import ctmn.petals.tile.TerrainNames
import ctmn.petals.tile.TileActor
import ctmn.petals.unit.TerrainPropsPack
import ctmn.petals.unit.UnitActor
import ctmn.petals.unit.tiledX
import ctmn.petals.unit.tiledY
import ctmn.petals.utils.unTiled
import com.badlogic.gdx.graphics.g2d.Batch
import com.badlogic.gdx.graphics.g2d.TextureAtlas.AtlasRegion
import com.badlogic.gdx.scenes.scene2d.Actor
import com.badlogic.gdx.utils.Array
import ctmn.petals.newPlaySprite
import ctmn.petals.tile.isPassableAndFree

class TileHighlighter(val gui: PlayGUIStage) : Actor() {

    val green = gui.assets.atlases.getRegion("gui/green_tile_highlight")
    val yellow = gui.assets.atlases.getRegion("gui/yellow_tile_highlight")
    val red = gui.assets.atlases.getRegion("gui/red_tile_highlight")

    private val sprite = newPlaySprite(green)

    private val tilesPos = Array<Pair<Int, Int>>()

    override fun draw(batch: Batch, parentAlpha: Float) {
        super.draw(batch, parentAlpha)

        for (tilePos in tilesPos) {
            sprite.setPosition(tilePos.first.unTiled(), tilePos.second.unTiled())

            if (gui.playStage.getTile(tilePos.first, tilePos.second)?.isPassableAndFree() == true)
                sprite.draw(batch)
        }
    }

    fun highlightSummoning(unitActor: UnitActor, range: Int) {
        if (unitActor.stage == null) throw IllegalStateException("$unitActor is not on the stage.")

        val matrix = gui.playStage.getMovementGrid(range, unitActor.tiledX, unitActor.tiledY, TerrainPropsPack.ability)
        for (x in matrix.indices) {
            for (y in matrix[x].indices) {
                if (matrix[x][y] != 0)
                    tilesPos.add(x to y)
            }
        }

        val tilePosToRemove = Array<Pair<Int, Int>>()
        for (tilePos in tilesPos) {
            val tile = gui.playStage.getTile(tilePos.first, tilePos.second) ?: break

            if (tile.terrain == TerrainNames.impassable || tile.isOccupied ||
                (tile.tiledX == unitActor.tiledX && tile.tiledY == unitActor.tiledY))
                tilePosToRemove.add(tilePos)
        }
        for (tilePos in tilePosToRemove)
            tilesPos.removeValue(tilePos, false)
    }

    fun highlight(tile: TileActor, highlightTexture: AtlasRegion) {
        tilesPos.add(tile.tiledX to tile.tiledY)

        sprite.setRegion(highlightTexture)
    }

    fun highlight(tiles: Array<TileActor>) {
        for (tile in tiles) {
            tilesPos.add(tile.tiledX to tile.tiledY)
        }
    }

    fun clearHighlights() {
        tilesPos.clear()

        sprite.setRegion(green)
    }
}