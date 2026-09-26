package ctmn.petals.tile

import com.badlogic.gdx.Gdx
import com.google.gson.JsonParser
import ctmn.petals.assets
import java.util.*
import kotlin.collections.HashMap

/**
 * Tile catalog. The authoritative list of tile ids and their terrain comes from
 * `tiles.json`, a small data file generated from the packed tiles atlas. That keeps the
 * tile definitions available to the headless dedicated server, which has no GL context and
 * therefore cannot read a [com.badlogic.gdx.graphics.g2d.TextureAtlas].
 */
object TileData {

    val tiles = HashMap<String, Tile>()

    private var loaded = false

    fun ensureLoaded() {
        if (loaded) return
        parseTiles()
        loaded = true
    }

    fun parseTiles() {
        val catalog = runCatching { Gdx.files.internal("tiles.json").readString() }.getOrNull()
        if (catalog != null) {
            loadCatalog(catalog)
            return
        }

        // Fallback for builds without the catalog: read the summer atlas region list.
        for (region in assets.tilesAtlas.regions) {
            val a = region.name.split("/")
            if (a.size != 2)
                continue

            tiles[a[1]] = Tile(a[1], a[0])
        }
    }

    private fun loadCatalog(json: String) {
        val array = JsonParser.parseString(json.trimStart('\uFEFF')).asJsonArray
        for (element in array) {
            val obj = element.asJsonObject
            val name = obj.get("name").asString
            val terrain = obj.get("terrain").asString
            tiles[name] = Tile(name, terrain)
        }
    }

    /** True when a tile region named [name] exists for [terrain]. Works headless. */
    fun hasRegion(terrain: String, name: String): Boolean {
        ensureLoaded()
        return tiles[name]?.terrain == terrain
    }

    fun add(tile: Tile) {
        tiles[tile.name.toLowerCase(Locale.ROOT)] = tile
    }

    fun get(name: String): Tile? {
        ensureLoaded()
        return tiles[name] ?: tiles[name.split("_")[0]]
    }

    fun getOrNull(name: String): Tile? {
        ensureLoaded()
        return tiles[name]
    }
}
