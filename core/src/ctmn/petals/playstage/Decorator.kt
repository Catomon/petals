package ctmn.petals.playstage

import com.badlogic.gdx.Gdx
import ctmn.petals.playscreen.PlayScreen
import ctmn.petals.playscreen.playStageOrNull
import ctmn.petals.playscreen.selfName
import ctmn.petals.tile.TileActor
import ctmn.petals.tile.TileData
import ctmn.petals.tile.Tiles

class Decorator {
    private val combinableSuffixes =
        arrayOf("l", "r", "t", "b", "lb", "lr", "lrb", "lrt", "lrtb", "lt", "ltb", "r", "rb", "rt", "rtb", "t", "tb")

    fun decorate(playStage: PlayStage) {
        //addMissingTiles()

        for (tile in playStage.getAllTiles()) {
            if (tile.isCombinable() && tile.layer == 1) {
                tile.combine()
            }
        }
    }

    fun combineTile(tileActor: TileActor) {
        tileActor.combine()
    }

    /** Combines tile with other equal tiles.
     * The tile should have textures with the name containing one of suffixes [combinableSuffixes] */
    private fun TileActor.combine() {
        if (!isCombinable()) return
        val playStage = playStageOrNull ?: throw IllegalStateException("Tile is not on the Stage")

        val nameNoSuff = nameNoSuffix()

        with(playStage) {
            /* make suffix based on similar neighbour tiles */

            // returns true if there are similar neighbour tile or if there are null neighbour at same level
            fun isSimilarTile(tiledX: Int, tiledY: Int): Boolean {
                val hasSameNeighbour = getAllTiles().firstOrNull {
                    val itNameNoSuffix = it.nameNoSuffix()
                    val bothNames = arrayOf(nameNoSuff, itNameNoSuffix)
                    val isSameName = itNameNoSuffix == nameNoSuff
                            || bothNames[0] == Tiles.WATER && bothNames[1] == Tiles.DEEPWATER
                    isSameName && it.tiledX == tiledX && it.tiledY == tiledY
                }
                return hasSameNeighbour != null || getTile(tiledX, tiledY, layer) == null
            }

            val left = if (isSimilarTile(tiledX - 1, tiledY)) "l" else ""
            val right = if (isSimilarTile(tiledX + 1, tiledY)) "r" else ""
            val top = if (isSimilarTile(tiledX, tiledY + 1)) "t" else ""
            val bottom = if (isSimilarTile(tiledX, tiledY - 1)) "b" else ""

            val suff = "_$left$right$top$bottom"

            /* Combine the tile name with the suffix and find a texture for the tile, then apply the texture */

            var combinedName = tileComponent.name + suff

            // if swamp_lrtb not found else just swamp
            if (suff == "_lrtb") {
                if (assets.atlases.findRegion("tiles/${terrain}/" + combinedName) == null)
                    combinedName = combinedName.removeSuffix(suff)
            }

            // find texture for combined option, else, try flipping existing texture and throw exception if no textures found
            if (assets.atlases.findRegion("tiles/${terrain}/" + combinedName) != null) {
                tileComponent.name = combinedName
                initView()
            } else {
                // option if im too lazy to make all textures but have some that can be flipped or rotated
                val cheapCombinedName = tileComponent.name +
                        when {
                            combinedName.endsWith("_lrb") -> "_rtb"
                            combinedName.endsWith("_ltb") -> "_rtb"
                            combinedName.endsWith("_lrt") -> "_rtb"
                            combinedName.endsWith("_lb") -> "_rb"
                            combinedName.endsWith("_rt") -> "_rb"
                            combinedName.endsWith("_lt") -> "_rb"
                            combinedName.endsWith("_lr") -> "_tb"
                            combinedName.endsWith("_b") -> "_r"
                            combinedName.endsWith("_t") -> "_r"
                            combinedName.endsWith("_l") -> "_r"

                            else -> return // "swamp" with no neighbours I guess
                        }

                if (assets.atlases.findRegion("tiles/${terrain}/" + cheapCombinedName) == null) {
                    Gdx.app.error(
                        this@Decorator::class.simpleName,
                        "Combining: Tile texture not found ${"tiles/${terrain}/" + cheapCombinedName}"
                    )
                    return
                }

                tileComponent.name = cheapCombinedName
                initView()

                when {
                    combinedName.endsWith("_lrb") -> sprite.rotation = 270f
                    combinedName.endsWith("_ltb") -> sprite.rotation = 180f
                    combinedName.endsWith("_lrt") -> sprite.rotation = 90f

                    combinedName.endsWith("_lb") -> sprite.setFlip(true, false)
                    combinedName.endsWith("_rt") -> sprite.setFlip(false, true)
                    combinedName.endsWith("_lt") -> sprite.setFlip(true, true)

                    combinedName.endsWith("_lr") -> sprite.rotation = 90f

                    combinedName.endsWith("_b") -> sprite.rotation = 270f
                    combinedName.endsWith("_t") -> sprite.rotation = 90f
                    combinedName.endsWith("_l") -> sprite.rotation = 180f
                }
            }

            // if there are "_back" texture, put a tile with it to layer 0
            val backTexture = assets.atlases.findRegion("tiles/$terrain/${nameNoSuff}_back")
            if (backTexture != null && suff != "_lrtb") {

                //getTile(tiledX, tiledY, 0)?.remove()

                if (getTile(tiledX, tiledY, 0) == null) {
                    //val tileData = playScreen.tilesData.get("${nameNoSuff}_back")!!
                    val tileData = TileData.get(nameNoSuff)!!
                    val tile = TileActor(tileData.name, tileData.terrain)
                    tile.initView()
                    tile.tileComponent.layer = 0
                    tile.setPosition(x, y)
                    addActor(tile)
                }
            }
        }
    }

    private fun TileActor.nameNoSuffix(): String {
        var nameNoSuffix = selfName
        combinableSuffixes.forEach { nameNoSuffix = nameNoSuffix.removeSuffix("_$it") }
        "abcdefghijklmnop".forEach { nameNoSuffix = nameNoSuffix.removeSuffix("_$it") }

        return nameNoSuffix
    }

    private fun TileActor.isCombinable(): Boolean {
        //var nameNoSuffix = selfName
        //"abcdefghijklmnop".forEach { nameNoSuffix = nameNoSuffix.removeSuffix("_$it") }

        combinableSuffixes.forEach {
            if (assets.atlases.findRegion("tiles/${terrain}/" + selfName + "_$it") != null)
                return true
        }

        return false
    }

    // add grass tile if a tile in layer 1(main) is missing
    fun addMissingTiles(playScreen: PlayScreen, playStage: PlayStage) {
        for (x in 0 until playStage.tiledWidth()) {
            for (y in 0 until playStage.tiledHeight()) {
                if (playStage.getTile(x, y) == null) {
                    val tileData = playScreen.tilesData.get("grass")!!
                    val tile = TileActor(tileData.name, tileData.terrain)
                    tile.initView()
                    tile.tileComponent.layer = 1
                    tile.setPosition(x, y)
                    playStage.addActor(tile)
                }
            }
        }
    }
}