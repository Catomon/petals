package ctmn.petals.tile

import ctmn.petals.assets
import java.util.*
import kotlin.collections.HashMap

object TileData {

    val tiles = HashMap<String, Tile>()

    init {
        parseTiles()
    }

    fun parseTiles() {
        for (region in assets.tilesAtlas.regions) {
            val a = region.name.split("/")
            if (a.size != 2)
                continue

            tiles[a[1]] = Tile(a[1], a[0])
        }
    }

    fun add(tile: Tile) {
        tiles[tile.name.toLowerCase(Locale.ROOT)] = tile
    }

    fun get(name: String): Tile? {
        return tiles[name] ?: tiles[name.split("_")[0]]
    }

    fun getOrNull(name: String): Tile? {
        return tiles[name]
    }
}
