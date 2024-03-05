package ctmn.petals.map

import ctmn.petals.playstage.PlayStage
import ctmn.petals.playstage.getTiles
import ctmn.petals.playstage.isOutOfMap
import ctmn.petals.playstage.tiledDst
import ctmn.petals.tile.*
import ctmn.petals.tile.components.PlayerIdComponent
import ctmn.petals.utils.getSurroundingTiles
import ctmn.petals.utils.getTilesInRange
import kotlin.random.Random

class MapGenerator {

    //20 15
    val width = 15
    val height = 15

    val bases = 2

    val blueCrystalsAmount = 2
    val lifeCrystalsAmount = 1

    val trees = 60
    val mountains = 15
    val flowers = 30

    val mountainTileRandomness = 2

    val crystalPlaceRangeMin = 2 //maxOf(width, height) / 2 - 5
    val crystalPlaceRangeMax = maxOf(width, height) / 2

    val groundTileData = TileData.get("grass") ?: throw IllegalArgumentException("tile data not found")
    val treeTileData = TileData.get("tree") ?: throw IllegalArgumentException("tile data not found")
    val mountainTileData = TileData.get("mountain") ?: throw IllegalArgumentException("tile data not found")
    val flowerTileData = TileData.get("grass_flowers") ?: throw IllegalArgumentException("tile data not found")
    val baseTileData = TileData.get("life_crystal") ?: throw IllegalArgumentException("tile data not found")
    val crystalTileData = TileData.get("crystal") ?: throw IllegalArgumentException("tile data not found")
    val lifeCrystalTileData = TileData.get("life_crystal") ?: throw IllegalArgumentException("tile data not found")

    private lateinit var playStage: PlayStage

    fun generate(playStage: PlayStage) {
        this.playStage = playStage

        for (x in 0 until width) {
            for (y in 0 until height) {
                playStage.addActor(TileActor(groundTileData.name, groundTileData.terrain, 1, x, y))
            }
        }

        for (i in 1..flowers) {
            val tile = placeFlowerRandomly()
            tile.remove()
            tile.layer = 1
            playStage.addActor(tile)
        }

        for (i in 1..trees) {
            placeTreeRandomly()
        }

        for (i in 1..mountains) {
            placeTileRandomly(mountainTileData, mountainTileRandomness)
        }

        placeBases()

        placeCrystals(blueCrystalsAmount, crystalTileData)
        placeCrystals(lifeCrystalsAmount, lifeCrystalTileData)
    }

    private fun placeCrystals(
        amount: Int,
        crystalTileData: Tile,
    ) {
        val amountPerBase = if (amount > 1) amount / 2 else amount

        repeat(amountPerBase) {
            val bases =
                playStage.getTiles().filter { it.terrain == TerrainNames.base && it.has(PlayerIdComponent::class.java) }
            for (baseTile in arrayOf(bases.first())) {
                val tile = playStage.getTilesInRange(baseTile.tiledX, baseTile.tiledY, crystalPlaceRangeMax)
                    .filter {
                        IntRange(crystalPlaceRangeMin, crystalPlaceRangeMax).contains(
                            tiledDst(
                                baseTile.tiledX,
                                baseTile.tiledY,
                                it.tiledX,
                                it.tiledY
                            )
                        )
                                && !it.isCrystal && !it.isBase
                    }.random()

                val x = tile.tiledX
                val y = tile.tiledY

                val baseOne = TileActor(crystalTileData.name, crystalTileData.terrain, 1, x, y)
                val baseTwo = TileActor(crystalTileData.name, crystalTileData.terrain, 1, width - x - 1, height - y - 1)

                playStage.addActor(baseOne)
                playStage.addActor(baseTwo)
            }
        }
    }

    private fun placeBases() {
        if (bases == 2) {

            val freeX = Random.nextInt(0, 1)
            var x = if (freeX == 0) Random.nextInt(1, width / 4) else Random.nextInt(1, width - 1)
            if (freeX == 0) {
                var sideX = Random.nextInt(0, 1)
                if (sideX == 1) x = width - x
            }

            var y = if (freeX == 1) Random.nextInt(1, height / 4) else Random.nextInt(1, height - 1)
            if (freeX == 1) {
                var sideY = Random.nextInt(0, 1)
                if (sideY == 1) y = height - y
            }

            val baseOne = TileActor(baseTileData.name, baseTileData.terrain, 1, x, y)
            val baseTwo = TileActor(baseTileData.name, baseTileData.terrain, 1, width - x - 1, height - y - 1)

            setPlayerForCapturableTile(baseOne, 1)
            setPlayerForCapturableTile(baseTwo, 2)

            playStage.addActor(baseOne)
            playStage.addActor(baseTwo)
        }
    }

    private fun placeFlowerRandomly(): TileActor {
        var placeTries = 0
        do {
            placeTries++
            // 0 - random, 1 - near a tree, 2 - ner 2> trees
            val placementRule = Random.nextInt(0, 2)

            var x = Random.nextInt(0, width)
            var y = Random.nextInt(0, height)

            when (placementRule) {
                1 -> {
                    val neighbour = playStage.tileLayers[2]?.children?.shuffled()
                        ?.firstOrNull { (it as TileActor).terrain == flowerTileData.terrain } as TileActor? ?: continue
                    var tries = 0
                    do {
                        x = Random.nextInt(neighbour.tiledX - 1, neighbour.tiledX + 1)
                        y = Random.nextInt(neighbour.tiledY - 1, neighbour.tiledY + 1)
                        tries++

                        if (playStage.getTile(x, y, 2)?.terrain != flowerTileData.terrain && !playStage.isOutOfMap(
                                x,
                                y
                            )
                        ) break
                    } while (tries < 32)
                }

                2 -> {
                    val neighbour = playStage.tileLayers[2]?.children?.shuffled()?.firstOrNull {
                        (it as TileActor).terrain == flowerTileData.terrain && !playStage.getSurroundingTiles(
                            it.tiledX,
                            it.tiledY
                        ).isEmpty
                    } as TileActor? ?: continue
                    var tries = 0
                    do {
                        x = Random.nextInt(neighbour.tiledX - 1, neighbour.tiledX + 1)
                        y = Random.nextInt(neighbour.tiledY - 1, neighbour.tiledY + 1)
                        tries++

                        if (playStage.getTile(x, y, 2)?.terrain != flowerTileData.terrain && !playStage.isOutOfMap(
                                x,
                                y
                            )
                        ) break
                    } while (tries < 32)
                }
            }

            //done
            if (playStage.getTile(x, y, 2)?.terrain != flowerTileData.terrain && !playStage.isOutOfMap(x, y)) {
                val flowerTile = TileActor(flowerTileData.name, flowerTileData.terrain, 2, x, y)
                playStage.addActor(flowerTile)

                return flowerTile
            }

        } while (placeTries < 32)

        throw IllegalStateException("could not put a flower :(")
    }

    private fun placeTreeRandomly() {
        var placeTries = 0
        do {
            placeTries++
            // 0 - random, 1 - near a tree, 2 - ner 2> trees
            val placementRule = Random.nextInt(0, 2)

            var x = Random.nextInt(0, width)
            var y = Random.nextInt(0, height)

            when (placementRule) {
                1 -> {
                    val neighbour =
                        playStage.getTiles().apply { shuffle() }.firstOrNull { it.terrain == treeTileData.terrain }
                            ?: continue
                    var tries = 0
                    do {
                        x = Random.nextInt(neighbour.tiledX - 1, neighbour.tiledX + 1)
                        y = Random.nextInt(neighbour.tiledY - 1, neighbour.tiledY + 1)
                        tries++

                        if (playStage.getTile(x, y)?.terrain != treeTileData.terrain && !playStage.isOutOfMap(
                                x,
                                y
                            )
                        ) break
                    } while (tries < 32)
                }

                2 -> {
                    val neighbour = playStage.getTiles().apply { shuffle() }.firstOrNull {
                        it.terrain == treeTileData.terrain && !playStage.getSurroundingTiles(
                            it.tiledX,
                            it.tiledY
                        ).isEmpty
                    } ?: continue
                    var tries = 0
                    do {
                        x = Random.nextInt(neighbour.tiledX - 1, neighbour.tiledX + 1)
                        y = Random.nextInt(neighbour.tiledY - 1, neighbour.tiledY + 1)
                        tries++

                        if (playStage.getTile(x, y)?.terrain != treeTileData.terrain && !playStage.isOutOfMap(
                                x,
                                y
                            )
                        ) break
                    } while (tries < 32)
                }
            }

            //done
            if (playStage.getTile(x, y)?.terrain != treeTileData.terrain && !playStage.isOutOfMap(x, y)) {
                val treeTile = TileActor(treeTileData.name, treeTileData.terrain, 1, x, y)
                playStage.addActor(treeTile)

                break
            }

        } while (placeTries < 32)
    }

    private fun placeTileRandomly(tile: Tile, randomness: Int = 0) {
        var placeTries = 0
        do {
            placeTries++
            val placementRule = Random.nextInt(0, 2 + randomness)

            var x = Random.nextInt(0, width)
            var y = Random.nextInt(0, height)

            when (placementRule) {
                1 -> {
                    val neighbour = playStage.getTiles().apply { shuffle() }
                        .firstOrNull { it.terrain == tile.terrain } ?: continue
                    var tries = 0
                    do {
                        x = Random.nextInt(neighbour.tiledX - 1, neighbour.tiledX + 1)
                        y = Random.nextInt(neighbour.tiledY - 1, neighbour.tiledY + 1)
                        tries++

                        if (playStage.getTile(x, y)?.terrain != tile.terrain && !playStage.isOutOfMap(x, y)) break
                    } while (tries < 32)
                }

                2 -> {
                    val neighbour = playStage.getTiles().apply { shuffle() }
                        .firstOrNull {
                            it.terrain == tile.terrain && !playStage.getSurroundingTiles(
                                it.tiledX,
                                it.tiledY
                            ).isEmpty
                        }
                        ?: continue
                    var tries = 0
                    do {
                        x = Random.nextInt(neighbour.tiledX - 1, neighbour.tiledX + 1)
                        y = Random.nextInt(neighbour.tiledY - 1, neighbour.tiledY + 1)
                        tries++

                        if (playStage.getTile(x, y)?.terrain != tile.terrain && !playStage.isOutOfMap(x, y)) break
                    } while (tries < 32)
                }
            }

            if (playStage.getTile(x, y)?.terrain != tile.terrain && !playStage.isOutOfMap(x, y)) {
                val tileActor = TileActor(tile.name, tile.terrain, 1, x, y)
                playStage.addActor(tileActor)

                break
            }

        } while (placeTries < 32)
    }
}