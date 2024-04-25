package ctmn.petals.editor

import com.badlogic.gdx.Gdx
import com.badlogic.gdx.graphics.g2d.Sprite
import com.badlogic.gdx.utils.Array
import ctmn.petals.assets

class CanvasActorsPackage {

    val canvasActors = Array<CanvasActor>()
    val canvasActorsFiltered = Array<CanvasActor>()

    var minTileSize = 64f

    init {
        for (region in assets.tilesAtlas.regions) {
            val name =
                if (region.name.contains("/"))
                    region.name.split("/")[1]
                else
                    region.name

            val sprite = Sprite(region).apply {
                setSize(tileSize / minTileSize * region.regionWidth, tileSize / minTileSize * region.regionHeight)
            }

            if (name.contains("goblin_den") || name.contains("pixie_nest")) continue

            val tile = CanvasActor(name, sprite)
            canvasActors.add(tile)
        }

        canvasActors.forEach { actor ->
            if (!TileCombiner.hasCombinationSuffix(actor.name)) {
                assets.tilesAtlas.regions.filter { region ->
                    (if (region.name.contains("/"))
                        region.name.split("/")[1]
                    else
                        region.name) == actor.name
                }.let { regions ->
                    if (regions.size <= 1)
                        canvasActorsFiltered.add(actor)
                    else
                        if (canvasActorsFiltered.find { it.name == actor.name } == null)
                            canvasActorsFiltered.add(actor)
                }
            }
        }
    }

    fun find(name: String): CanvasActor? = canvasActors.find { it.name == name }

    fun get(name: String): CanvasActor {
        return canvasActors.firstOrNull { it.name == name } ?: let {
            Gdx.app.log(this::class.simpleName, "Tile $name not found")
            canvasActors.firstOrNull { it.name == "tile" } ?: throw IllegalStateException("Not even \"tile\" found")
        }
    }
}