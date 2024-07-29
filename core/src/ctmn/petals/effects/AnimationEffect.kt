package ctmn.petals.effects

import com.badlogic.gdx.graphics.g2d.TextureAtlas
import com.badlogic.gdx.utils.Array
import ctmn.petals.Const
import ctmn.petals.newPlaySprite
import ctmn.petals.utils.RegionAnimation

class AnimationEffect(regions: Array<TextureAtlas.AtlasRegion>, frameDuration: Float = 0.05f) : EffectActor() {
    init {
        animation = RegionAnimation(frameDuration, regions)
        sprite = newPlaySprite(animation!!.currentFrame)
        lifeTime = animation!!.animationDuration

        setSize(sprite.width, sprite.height)

        if (sprite.width < Const.TILE_SIZE) {
            sprite.setSize(Const.TILE_SIZE, Const.TILE_SIZE)
            width = Const.TILE_SIZE
            height = Const.TILE_SIZE
        }
    }
}
