package ctmn.petals.utils

import com.badlogic.gdx.graphics.g2d.Sprite
import com.badlogic.gdx.graphics.g2d.TextureAtlas
import com.badlogic.gdx.utils.Array

class AnimatedSprite(
    var animation: RegionAnimation,
) : Sprite(animation.currentFrame) {

    constructor(regions: Array<TextureAtlas.AtlasRegion>, frameDuration: Float = 0.5f) : this(
        RegionAnimation(
            frameDuration,
            regions
        )
    )

    fun update(delta: Float) {
        animation.update(delta)
        setRegion(animation.currentFrame)
    }
}