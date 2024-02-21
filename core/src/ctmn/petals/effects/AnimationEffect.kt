package ctmn.petals.effects

import ctmn.petals.Const
import ctmn.petals.utils.RegionAnimation
import com.badlogic.gdx.graphics.g2d.TextureAtlas
import com.badlogic.gdx.utils.Array
import ctmn.petals.newPlaySprite

class AnimationEffect(regions: Array<TextureAtlas.AtlasRegion>, frameDuration: Float = 0.05f) : EffectActor() {
    init {
        animation = RegionAnimation(frameDuration, regions)
        sprite = newPlaySprite(animation!!.currentFrame)
        lifeTime = animation!!.animationDuration

        setSize(sprite.width, sprite.height)

        if (sprite.width < Const.TILE_SIZE) {
            sprite.setSize(Const.TILE_SIZE.toFloat(), Const.TILE_SIZE.toFloat())
            width = Const.TILE_SIZE.toFloat()
            height = Const.TILE_SIZE.toFloat()
        }
    }
}
