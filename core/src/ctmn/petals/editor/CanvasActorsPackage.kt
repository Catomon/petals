package ctmn.petals.editor

import com.badlogic.gdx.graphics.g2d.Sprite
import com.badlogic.gdx.utils.Array
import ctmn.petals.assets

class CanvasActorsPackage {

    val canvasActors = Array<CanvasActor>()

    var minTileSize = 64f

    init {
        for (region in assets.tilesAtlas.regions) {
            val name =
                if (region.name.contains("/"))
                    region.name.split("/")[1]
                else
                    region.name

            val sprite = Sprite(region).apply {
                setSize(width / minTileSize * region.regionWidth, height / minTileSize * region.regionHeight)
            }

            val tile = CanvasActor(name, sprite)
            canvasActors.add(tile)
        }
    }

    fun get(name: String): CanvasActor {
        return canvasActors.firstOrNull { it.name == name } ?: throw IllegalStateException("Tile not found")
    }
}