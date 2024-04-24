package ctmn.petals.editor

import com.badlogic.gdx.Gdx
import ctmn.petals.tile.Tiles

class TileCombiner(val actorsPackage: CanvasActorsPackage) {

    private val combinableSuffixes =
        arrayOf("l", "r", "t", "b", "lb", "lr", "lrb", "lrt", "lrtb", "lt", "ltb", "r", "rb", "rt", "rtb", "t", "tb")

    fun combine(tile: CanvasActor) {
        if (tile.name.endsWith("_combinable")) {
            tile.name = tile.name.removeSuffix("_combinable")
            tile.sprite.set(actorsPackage.get(tile.name).sprite)
        }

        val nameNoSuff = tile.nameNoSuffix()

        if (actorsPackage.find(nameNoSuff + "_combinable") == null) return

        val stage = tile.stage as CanvasStage? ?: throw IllegalStateException("Tile is not on the Stage")

        tile.name = nameNoSuff
        tile.sprite.set(actorsPackage.get(tile.name).sprite)

        val tiledX = tile.x.toTilePos()
        val tiledY = tile.y.toTilePos()

        with(stage) {
            /* make suffix based on similar neighbour tiles */

            // returns true if there are similar neighbour tile or if there are null neighbour at same level
            fun isSimilarTile(tiledX: Int, tiledY: Int): Boolean {
                val hasSameNeighbour = getCanvasActors().firstOrNull {
                    val itNameNoSuffix = it.nameNoSuffix()
                    val bothNames = arrayOf(nameNoSuff, itNameNoSuffix)
                    val isSameName = itNameNoSuffix == nameNoSuff
                            || bothNames[0] == Tiles.WATER && bothNames[1] == Tiles.DEEPWATER
                    isSameName && it.x.toTilePos() == tiledX && it.y.toTilePos() == tiledY
                }
                return hasSameNeighbour != null || getActor(tiledX, tiledY, tile.layer) == null
            }

            val left = if (isSimilarTile(tiledX - 1, tiledY)) "l" else ""
            val right = if (isSimilarTile(tiledX + 1, tiledY)) "r" else ""
            val top = if (isSimilarTile(tiledX, tiledY + 1)) "t" else ""
            val bottom = if (isSimilarTile(tiledX, tiledY - 1)) "b" else ""

            val suff = "_$left$right$top$bottom"

            /* Combine the tile name with the suffix and find a texture for the tile, then apply the texture */

            var combinedName = tile.name + suff

            // if swamp_lrtb not found else just swamp
            if (suff == "_lrtb") {
                if (actorsPackage.find(combinedName) == null)
                    combinedName = combinedName.removeSuffix(suff)
            }

            // find texture for combined option, else, try flipping existing texture and throw exception if no textures found
            if (actorsPackage.find(combinedName) != null) {
                tile.name = combinedName
                tile.sprite.set(actorsPackage.get(combinedName).sprite)
            } else {
                // option if im too lazy to make all textures but have some that can be flipped or rotated
                val cheapCombinedName = tile.name +
                        when {
                            combinedName.contains("_lrb") -> "_rtb"
                            combinedName.contains("_ltb") -> "_rtb"
                            combinedName.contains("_lrt") -> "_rtb"
                            combinedName.contains("_lb") -> "_rb"
                            combinedName.contains("_rt") -> "_rb"
                            combinedName.contains("_lt") -> "_rb"
                            combinedName.contains("_lr") -> "_tb"
                            combinedName.contains("_b") -> "_r"
                            combinedName.contains("_t") -> "_r"
                            combinedName.contains("_l") -> "_r"

                            else -> return // "swamp" with no neighbours I guess
                        }

                if (actorsPackage.find(cheapCombinedName) == null) {
                    Gdx.app.error(
                        this@TileCombiner::class.simpleName,
                        "Combining: Tile not found: $cheapCombinedName}"
                    )
                    return
                }

                tile.name = cheapCombinedName
                tile.sprite.set(actorsPackage.get(cheapCombinedName).sprite)
                val sprite = tile.sprite

                sprite.setOriginCenter()

                when {
                    combinedName.contains("_lrb") -> sprite.rotation = 270f
                    combinedName.contains("_ltb") -> sprite.rotation = 180f
                    combinedName.contains("_lrt") -> sprite.rotation = 90f

                    combinedName.contains("_lb") -> sprite.setFlip(true, false)
                    combinedName.contains("_rt") -> sprite.setFlip(false, true)
                    combinedName.contains("_lt") -> sprite.setFlip(true, true)

                    combinedName.contains("_lr") -> sprite.rotation = 90f

                    combinedName.contains("_b") -> sprite.rotation = 270f
                    combinedName.contains("_t") -> sprite.rotation = 90f
                    combinedName.contains("_l") -> sprite.rotation = 180f
                }
            }

            val nameNoSuff = tile.nameNoSuffix()
            // if there are "_back" texture, put a tile with it to layer 0
            val backTexture = actorsPackage.find("${nameNoSuff}_back")
            if (backTexture != null && suff != "_lrtb") {

                //getTile(tiledX, tiledY, 0)?.remove()

                val backLayer = tile.layer - 1
                if (getActor(tiledX, tiledY, backLayer) == null) {
                    //val tileData = playScreen.tilesData.get("${nameNoSuff}_back")!!
                    val tileBack = actorsPackage.get(nameNoSuff).copy()
                    tileBack.layer = backLayer
                    tileBack.setPosition(tile.x, tile.y)
                    addActor(tileBack, backLayer)
                }
            }
        }

        tile.name = tile.nameNoSuffix()
    }

    private fun CanvasActor.nameNoSuffix(): String {
        var nameNoSuffix = name
        combinableSuffixes.forEach { nameNoSuffix = nameNoSuffix.removeSuffix("_$it") }
        "abcdefghijklmnop".forEach { nameNoSuffix = nameNoSuffix.removeSuffix("_$it") }

        return nameNoSuffix
    }

    private fun CanvasActor.isCombinable(): Boolean {
        combinableSuffixes.forEach {
            if (name.contains("_$it"))
                return true
        }

        return false
    }
}