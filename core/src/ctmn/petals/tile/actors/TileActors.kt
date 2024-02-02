package ctmn.petals.tile.actors

import ctmn.petals.tile.Terrain
import ctmn.petals.tile.TileActor
import ctmn.petals.tile.components.LifeTimeComponent
import ctmn.petals.tile.components.ReplaceWithComponent

const val BURNING_FOREST = "burning_forest"
const val BURNED_FOREST = "burned_forest"

val newBurnedForestTile get() = TileActor(BURNED_FOREST, Terrain.forest)

val newBurningForestTile get() = TileActor(BURNING_FOREST, Terrain.forest).apply {
    add(LifeTimeComponent(3f))
    add(ReplaceWithComponent(BURNED_FOREST))
}