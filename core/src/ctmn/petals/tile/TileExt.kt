package ctmn.petals.tile

import com.badlogic.gdx.scenes.scene2d.Actor
import ctmn.petals.player.fairy
import ctmn.petals.player.goblin
import ctmn.petals.playscreen.selfName
import ctmn.petals.playstage.PlayStage
import ctmn.petals.playstage.tiledDst
import ctmn.petals.tile.components.*

/** @returns true if tile is occupied by a unit */

val TileActor.isCapturable get() = terrain == Terrain.base || terrain == Terrain.crystals
val TileActor.isBase get() = terrain == Terrain.base
val TileActor.isCrystal get() = terrain == Terrain.crystals
val TileActor.cPlayerId get() = get(PlayerIdComponent::class.java)
val TileActor.cReplaceWith get() = get(ReplaceWithComponent::class.java)
val TileActor.cLifeTime get() = get(LifeTimeComponent::class.java)
val TileActor.cCapturing get() = get(CapturingComponent::class.java)

val TileActor.isOccupied get() = (stage as PlayStage).getUnit(tiledX, tiledY) != null

fun TileActor.isPassableAndFree(): Boolean {
    return !isOccupied && terrain != Terrain.impassable
}

fun TileActor.isPassable(): Boolean {
    return terrain != Terrain.impassable
}

fun TileActor.isImpassable(): Boolean {
    return terrain == Terrain.impassable
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

/** if species == null will be goblins by def. */
fun setPlayerForCapturableTile(crystalTile: TileActor, playerId: Int, pSpecies: String? = null) {
//    if (!base.has(TileViewComponent::class.java))
//        base.initView()

    if (playerId in 1..8)
        crystalTile.add(PlayerIdComponent(playerId))
    else
        crystalTile.del(PlayerIdComponent::class.java)

    val species = pSpecies ?: when (playerId) {
        1 -> fairy
        2 -> goblin
        else -> goblin
    }

    when (crystalTile.terrain) {
        Terrain.base -> {
            if (playerId in 1..8) {
                crystalTile.tileComponent.name = when (species) {
                    fairy -> Tiles.PIXIE_NEST
                    goblin -> Tiles.GOBLIN_DEN
                    else -> goblin
                }
            } else {
                crystalTile.tileComponent.name = Tiles.LIFE_CRYSTAL
                crystalTile.tileComponent.terrain = Terrain.crystals
            }
        }

        Terrain.crystals -> {
            when {
                crystalTile.selfName.contains(Tiles.LIFE_CRYSTAL) -> {
                    crystalTile.tileComponent.name = when (species) {
                        fairy -> Tiles.PIXIE_NEST
                        goblin -> Tiles.GOBLIN_DEN
                        else -> goblin
                    }

                    crystalTile.tileComponent.terrain = Terrain.base
                }

                crystalTile.selfName.contains(Tiles.CRYSTAL) -> {
                    crystalTile.tileComponent.name = when (species) {
                        fairy -> Tiles.CRYSTAL_FAIRY
                        goblin -> Tiles.CRYSTAL_GOBLIN
                        else -> goblin
                    }
                }
            }
        }
    }

    crystalTile.initView()

    if (species == fairy && playerId == 1 || species == goblin && playerId == 2) return

    // todo player color bases
}