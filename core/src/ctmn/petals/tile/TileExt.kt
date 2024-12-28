package ctmn.petals.tile

import com.badlogic.gdx.scenes.scene2d.Actor
import ctmn.petals.assets
import ctmn.petals.effects.AnimationEffect
import ctmn.petals.player.Player
import ctmn.petals.player.fairy
import ctmn.petals.player.goblin
import ctmn.petals.player.speciesList
import ctmn.petals.playscreen.playStageOrNull
import ctmn.petals.playscreen.selfName
import ctmn.petals.playstage.PlayStage
import ctmn.petals.playstage.tiledDst
import ctmn.petals.tile.components.*
import ctmn.petals.unit.playerColorName
import ctmn.petals.utils.centerX
import ctmn.petals.utils.centerY

/** @returns true if tile is occupied by a unit */

val TileActor.isBuilding get() = terrain == TerrainNames.building || terrain == TerrainNames.crystals
val TileActor.isCapturable get() = terrain == TerrainNames.base || terrain == TerrainNames.crystals
val TileActor.isBase get() = terrain == TerrainNames.base
val TileActor.isCrystal get() = terrain == TerrainNames.crystals
val TileActor.cPlayerId get() = get(PlayerIdComponent::class.java)
val TileActor.cReplaceWith get() = get(ReplaceWithComponent::class.java)
val TileActor.cLifeTime get() = get(LifeTimeComponent::class.java)
val TileActor.cCapturing get() = get(CapturingComponent::class.java)
val TileActor.cBaseBuilding get() = get(BaseBuildingComponent::class.java)
val TileActor.cDestroying get() = get(DestroyingComponent::class.java)
val TileActor.isOccupied get() = (stage as PlayStage).getUnit(tiledX, tiledY) != null
val TileActor.isWaterBase: Boolean
    get() {
        if (!isBase) return false
        val playStage = playStageOrNull ?: return false
        val backTerrain = playStage.getTile(tiledX, tiledY, layer - 1)?.terrain
        return backTerrain == TerrainNames.water || backTerrain == TerrainNames.deepwater || backTerrain == TerrainNames.lava
    }
val TileActor.isBurning: Boolean
    get() {
        return selfName == Tiles.BURNING_FOREST
    }
val TileActor.isFluid
    get() = terrain == TerrainNames.water
            || terrain == TerrainNames.deepwater
            || terrain == TerrainNames.lava
            || terrain == TerrainNames.swamp

fun TileActor.isPassableAndFree(): Boolean {
    return !isOccupied && terrain != TerrainNames.impassable
}

fun TileActor.isPassable(): Boolean {
    return terrain != TerrainNames.impassable
}

fun TileActor.isImpassable(): Boolean {
    return terrain == TerrainNames.impassable
}

fun TileActor.toSimpleString(): String {
    return "$tileName, $terrain, x: $tiledX, y: $tiledY"
}

fun TileActor.tiledDstTo(tile: TileActor): Int {
    return tiledDst(tiledX, tiledY, tile.tiledX, tile.tiledY)
}

fun Actor.isPlaceholderBaseTile(): Boolean = this is TileActor && arrayOf(
    "blue_base",
    "red_base",
    "green_base",
    "purple_base",
    "yellow_base",
    "orange_base",
    "pink_base",
    "brown_base"
).any { it == tileComponent.name }

fun placeholderBaseNameToPlayerId(name: String): Int = when (name) {
    "blue_base" -> 1
    "red_base" -> 2
    "green_base" -> 3
    "purple_base" -> 4
    "yellow_base" -> 5
    "orange_base" -> 6
    "pink_base" -> 7
    "brown_base" -> 8
    else -> throw IllegalArgumentException(name)
}

fun TileActor.nameNoTeamName(): String {
    return tileName.removeSuffix("_blue")
        .removeSuffix("_red")
        .removeSuffix("_green")
        .removeSuffix("_purple")
        .removeSuffix("_yellow")
        .removeSuffix("_orange")
        .removeSuffix("_pink")
        .removeSuffix("_brown")
}

/** if species == null will be goblins by def. */
fun setPlayerForCapturableTile(crystalTile: TileActor, playerId: Int, pSpecies: String? = null) {
//    if (!base.has(TileViewComponent::class.java))
//        base.initView()

    if (playerId in 1..8)
        crystalTile.add(PlayerIdComponent(playerId))
    else
        crystalTile.del(PlayerIdComponent::class.java)

    val colorName = playerColorName(playerId)
    val color = Player.colorById(playerId)

    val species = pSpecies ?: when (playerId) {
        1 -> fairy
        2 -> goblin
        else -> speciesList.random()
    }

    when (crystalTile.terrain) {
        TerrainNames.base -> {
            if (playerId in 1..8) {
                crystalTile.tileComponent.name = when (species) {
                    fairy -> Tiles.PIXIE_NEST
                    goblin -> Tiles.GOBLIN_DEN
                    else -> speciesList.random()
                }
            } else {
                crystalTile.tileComponent.name = Tiles.LIFE_CRYSTAL
                crystalTile.tileComponent.terrain = TerrainNames.crystals
            }
        }

        TerrainNames.crystals -> {
            when {
                crystalTile.selfName.contains(Tiles.LIFE_CRYSTAL) -> {
                    crystalTile.tileComponent.name = when (species) {
                        fairy -> Tiles.PIXIE_NEST
                        goblin -> Tiles.GOBLIN_DEN
                        else -> speciesList.random()
                    }

                    crystalTile.tileComponent.terrain = TerrainNames.base
                }

                crystalTile.selfName.contains(Tiles.CRYSTAL) -> {
                    crystalTile.tileComponent.name = when (species) {
                        fairy -> Tiles.CRYSTAL_FAIRY
                        goblin -> Tiles.CRYSTAL_GOBLIN
                        else -> speciesList.random()
                    }
                }
            }
        }
    }

    //if (crystalTile.isWaterBase) {
    //        val newName = crystalTile.tileComponent.name + "_water"
    //        if (assets.tilesAtlas.findRegion("base/new") != null) {
    //            crystalTile.tileComponent.name = newName
    //        }
    //    }

    if (colorName.isNotEmpty()) {
        if (crystalTile.isWaterBase)
            crystalTile.tileComponent.name += "_water"

        //if (crystalTile.isBase)

        if (assets.tilesAtlas.findRegion(crystalTile.terrain + "/" + crystalTile.tileComponent.name + "_$colorName") != null)
            crystalTile.tileComponent.name += "_$colorName"
    }

    crystalTile.initView()

//    if (colorName.isNotEmpty()) {
//        if (!crystalTile.isBase)
//            crystalTile.sprite.setColor(color)
//    }

//    if (species == fairy && playerId == 1 || species == goblin && playerId == 2) return
}

fun TileActor.cutGrass() {
    val tile = this
    if (tile.terrain == TerrainNames.grass || tile.terrain == TerrainNames.highgrass && !tile.selfName.endsWith("_cutoff")) {
        TileData.tiles.getOrDefault(tile.selfName + "_cutoff", null)?.let { tileData ->
            tile.tileComponent.name = tileData.name
            tile.tileComponent.terrain = tileData.terrain
            tile.initView()

            playStageOrNull?.addActor(AnimationEffect(assets.findAtlasRegions("effects/grass_cutoff"), 0.1f).apply {
                setPosition(tile.centerX, tile.centerY)
            })
        }
    }
}