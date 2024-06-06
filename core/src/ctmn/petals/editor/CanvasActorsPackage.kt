package ctmn.petals.editor

import com.badlogic.gdx.Gdx
import com.badlogic.gdx.graphics.g2d.Sprite
import com.badlogic.gdx.utils.Array
import ctmn.petals.assets
import ctmn.petals.tile.TerrainNames
import ctmn.petals.unit.Units

class CanvasActorsPackage {

    companion object {
        const val MARKERS_LAYER = 4
        const val UNITS_LAYER = 3
        const val OBJECTS_LAYER = 2
        const val GROUND_LAYER = 1
    }

    val canvasActors = Array<CanvasActor>()
    val canvasActorsFiltered = Array<CanvasActor>()

    var minTileSize = 16f

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
                arrayOf("mud").find { tile.name.startsWith(it) && terrain == TerrainNames.mud } != null -> {
                    tile.favouriteLayer = GROUND_LAYER
                }

                arrayOf("water").find { tile.name.startsWith(it) && terrain == TerrainNames.water } != null -> {
                    tile.favouriteLayer = GROUND_LAYER
                }

                arrayOf("grass", "grass_rain").find { tile.name == it && terrain == TerrainNames.grass } != null -> {
                    tile.favouriteLayer = GROUND_LAYER
                }

                arrayOf("road").find { tile.name.startsWith(it) && terrain == TerrainNames.roads } != null -> {
                    tile.favouriteLayer = GROUND_LAYER
                }

                arrayOf("swamp").find { tile.name.startsWith(it) && terrain == TerrainNames.swamp } != null -> {
                    tile.favouriteLayer = GROUND_LAYER
                }

                arrayOf("lava").find { tile.name.startsWith(it) && terrain == TerrainNames.lava } != null -> {
                    tile.favouriteLayer = GROUND_LAYER
                }

                arrayOf("deepwater").find { tile.name.startsWith(it) && terrain == TerrainNames.deepwater } != null -> {
                    tile.favouriteLayer = GROUND_LAYER
                }

                arrayOf("leader_spawn_point").find { tile.name.startsWith("leader_spawn_point") && terrain.isEmpty() } != null -> {
                    tile.favouriteLayer = MARKERS_LAYER
                }

                else -> tile.favouriteLayer = OBJECTS_LAYER
            }
        }

        for (unitName in Units.names) {
            val region = assets.unitsAtlas.findRegion(unitName)
            if (region == null) {
                Gdx.app.log(this::class.simpleName, "Unit region not found: $unitName")
                continue
            }
            val sprite = Sprite(region).apply {
                setSize(tileSize / minTileSize * region.regionWidth, tileSize / minTileSize * region.regionHeight)
            }

            val unit = CanvasActor(unitName, sprite)
            unit.favouriteLayer = UNITS_LAYER
            canvasActors.add(unit)
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