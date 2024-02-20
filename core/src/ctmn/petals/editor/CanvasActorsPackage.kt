package ctmn.petals.editor

import com.badlogic.gdx.graphics.g2d.Sprite
import com.badlogic.gdx.utils.Array
import ctmn.petals.assets

class CanvasActorsPackage {

    val canvasActors = Array<CanvasActor>()

    companion object {
        var minTileSize = 16f
    }

    init {
        for (region in assets.tilesAtlas.regions) {
            val name =
                if (region.name.contains("/"))
                    region.name.split("/")[1]
                else
                    region.name

            val tile = CanvasActor(name, Sprite(region))
            canvasActors.add(tile)
        }
    }

    fun get(name: String): CanvasActor {
        return canvasActors.firstOrNull { it.name == name } ?: throw IllegalStateException("Tile not found")
    }
}