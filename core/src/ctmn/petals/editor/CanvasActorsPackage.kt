package ctmn.petals.editor

import com.badlogic.gdx.Gdx
import com.badlogic.gdx.graphics.g2d.Sprite
import com.badlogic.gdx.utils.Array
import ctmn.petals.assets
import ctmn.petals.tile.TerrainNames

class CanvasActorsPackage {

    val canvasActors = Array<CanvasActor>()
    val canvasActorsFiltered = Array<CanvasActor>()

    var minTileSize = 64f

    lateinit var missingTilePlaceholder: CanvasActor

    init {
        for (region in assets.tilesAtlas.regions) {
            var terrain = ""
            val name =
                if (region.name.contains("/")) {
                    terrain = region.name.split("/")[0]
                    region.name.split("/")[1]
                } else
                    region.name

            val sprite = Sprite(region).apply {
                setSize(tileSize / minTileSize * region.regionWidth, tileSize / minTileSize * region.regionHeight)
            }

            if (name.contains("goblin_den") || name.contains("pixie_nest")) continue

            if (name == "tile") {
                missingTilePlaceholder = CanvasActor(name, sprite)
                continue
            }

            val tile = CanvasActor(name, sprite)
            canvasActors.add(tile)

            when {
                arrayOf("water").find { tile.name.startsWith(it) && terrain == TerrainNames.water } != null -> {
                    tile.favouriteLayer = 1
                }

                arrayOf("grass", "grass_rain").find { tile.name == it && terrain == TerrainNames.grass } != null -> {
                    tile.favouriteLayer = 1
                }

                arrayOf("road").find { tile.name.startsWith(it) && terrain == TerrainNames.roads } != null -> {
                    tile.favouriteLayer = 1
                }

                arrayOf("swamp").find { tile.name.startsWith(it) && terrain == TerrainNames.swamp } != null -> {
                    tile.favouriteLayer = 1
                }

                arrayOf("lava").find { tile.name.startsWith(it) && terrain == TerrainNames.lava } != null -> {
                    tile.favouriteLayer = 1
                }

                arrayOf("deepwater").find { tile.name.startsWith(it) && terrain == TerrainNames.deepwater } != null -> {
                    tile.favouriteLayer = 1
                }

                arrayOf("leader_spawn_point").find { tile.name == it && terrain.isEmpty() } != null -> {
                    tile.favouriteLayer = 3
                }

                else -> tile.favouriteLayer = 2
            }
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

            missingTilePlaceholder
        }
    }
}