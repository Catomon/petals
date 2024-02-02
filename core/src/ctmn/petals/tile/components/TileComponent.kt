package ctmn.petals.tile.components

import com.badlogic.ashley.core.Component

data class TileComponent(
    var name: String,
    var terrain: String,
    var layer: Int = 1,
    var tiledX: Int = 0,
    var tiledY: Int = 0,
) : Component