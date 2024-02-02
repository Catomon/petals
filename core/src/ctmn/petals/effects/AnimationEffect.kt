package ctmn.petals.effects

import ctmn.petals.GameConst
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

        if (sprite.width < GameConst.TILE_SIZE) {
            sprite.setSize(GameConst.TILE_SIZE.toFloat(), GameConst.TILE_SIZE.toFloat())
            width = GameConst.TILE_SIZE.toFloat()
            height = GameConst.TILE_SIZE.toFloat()
        }
    }
}
