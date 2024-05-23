package ctmn.petals.unit.component

import ctmn.petals.Const
import ctmn.petals.utils.RegionAnimation
import com.badlogic.gdx.graphics.g2d.Sprite
import com.badlogic.gdx.graphics.g2d.SpriteBatch
import com.badlogic.gdx.graphics.g2d.TextureAtlas
import com.badlogic.gdx.graphics.g2d.TextureRegion
import com.badlogic.gdx.utils.Array
import ctmn.petals.Const.TILE_SIZE

class AnimationViewComponent(var animation: RegionAnimation) : ViewComponent() {

    val sprite = Sprite(animation.currentFrame)
    var isAnimate = false

    constructor(frameDuration: Float, regions: Array<TextureAtlas.AtlasRegion>) : this(
        RegionAnimation(
            frameDuration,
            regions
        )
    )

    override fun update(delta: Float) {
        animation.update(delta)

        val region: TextureRegion =
            if (isAnimate) {
                if (animation.currentFrame == animation.keyFrames.first())
                    isAnimate = false

                animation.currentFrame
            } else {
                animation.stateTime = Const.UNIT_ANIMATION_FRAME_DURATION

                animation.keyFrames.first()
            }

        sprite.setRegion(region)
        if (region.regionHeight / 4 == 64 || region.regionWidth / 4 == 64) {
            sprite.setSize(64f, 64f)
            sprite.setOriginCenter()
        } else {
            sprite.setSize(32f, 32f)
        }

        sprite.setFlip(flipX, flipY)
    }

    override fun draw(batch: SpriteBatch) {
        sprite.draw(batch)
    }

    override fun setPosition(x: Float, y: Float) {
        sprite.setPosition(x - sprite.width / 2f, y - sprite.height / 2f)
    }
}