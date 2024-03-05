package ctmn.petals.unit.component

import ctmn.petals.Const
import ctmn.petals.utils.RegionAnimation
import com.badlogic.gdx.graphics.g2d.Sprite
import com.badlogic.gdx.graphics.g2d.SpriteBatch
import com.badlogic.gdx.graphics.g2d.TextureAtlas
import com.badlogic.gdx.utils.Array

class AnimationViewComponent(var animation: RegionAnimation) : ViewComponent() {

    val sprite = Sprite(animation.currentFrame)
    var isAnimate = false

    constructor(frameDuration: Float, regions: Array<TextureAtlas.AtlasRegion>) : this(RegionAnimation(frameDuration, regions))

    override fun update(delta: Float) {
        animation.update(delta)

        if (isAnimate) {
            sprite.setRegion(animation.currentFrame)

            if (animation.currentFrame == animation.keyFrames.first())
                isAnimate = false
        } else {
            sprite.setRegion(animation.keyFrames.first())
            animation.stateTime = Const.UNIT_ANIMATION_FRAME_DURATION
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