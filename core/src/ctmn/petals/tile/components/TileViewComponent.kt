package ctmn.petals.tile.components

import com.badlogic.ashley.core.Component
import com.badlogic.gdx.graphics.g2d.Sprite
import ctmn.petals.utils.RegionAnimation

class TileViewComponent(
    val sprite: Sprite,
    var animation: RegionAnimation? = null,
) : Component