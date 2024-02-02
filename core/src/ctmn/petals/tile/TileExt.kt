package ctmn.petals.tile

import ctmn.petals.player.fairy
import ctmn.petals.player.goblin
import ctmn.petals.playscreen.selfName
import ctmn.petals.playstage.PlayStage
import ctmn.petals.playstage.tiledDst
import ctmn.petals.tile.components.*

/** @returns true if tile is occupied by a unit */

val TileActor.isCapturable get() =  terrain == Terrain.base || terrain == Terrain.crystals
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

/** if species == null will be goblins by def. */
fun setTileCrystalPlayer(crystalTile: TileActor, playerId: Int, pSpecies: String? = null) {
//    if (!base.has(TileViewComponent::class.java))
//        base.initView()

    crystalTile.add(PlayerIdComponent(playerId))

    val species = pSpecies ?: when (playerId) {
        1 -> fairy
        2 -> goblin
        else -> goblin
    }

    when (crystalTile.terrain) {
        Terrain.base -> {
            crystalTile.tileComponent.name = when (species) {
                fairy -> Tiles.PIXIE_NEST
                goblin -> Tiles.GOBLIN_DEN
                else -> goblin
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