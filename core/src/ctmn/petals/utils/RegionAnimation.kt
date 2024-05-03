package ctmn.petals.utils

import com.badlogic.gdx.graphics.g2d.Animation
import com.badlogic.gdx.graphics.g2d.TextureAtlas
import com.badlogic.gdx.graphics.g2d.TextureRegion
import com.badlogic.gdx.utils.Array

class RegionAnimation(frameDuration: Float, regions: Array<TextureAtlas.AtlasRegion>) : Animation<TextureRegion>(frameDuration, regions) {

    var stateTime = 0f
    val currentFrame get() = getKeyFrame(stateTime)

    /** 1f means animation is at the last frame*/
    val progressLast get() = (stateTime + frameDuration) / animationDuration

    /** 1f means animation is ended */
    val progress get() = stateTime / animationDuration

    init {
        playMode = PlayMode.LOOP
    }

    fun update(delta: Float) {
        stateTime += delta
    }

    fun setFrames(frames: Array<TextureAtlas.AtlasRegion>) {
        for (i in keyFrames.indices) {
            keyFrames[i] = frames[i]
        }
    }

    fun setKeyFrames(keyFrames: Array<TextureAtlas.AtlasRegion>, frameDuration: Float = this.frameDuration) {
        this.keyFrames
        this.frameDuration = frameDuration
    }
}