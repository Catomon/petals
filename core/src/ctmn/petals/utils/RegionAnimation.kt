package ctmn.petals.utils

import com.badlogic.gdx.graphics.g2d.Animation
import com.badlogic.gdx.graphics.g2d.TextureAtlas
import com.badlogic.gdx.graphics.g2d.TextureRegion
import com.badlogic.gdx.utils.Array

class RegionAnimation(frameDuration: Float, regions: Array<TextureAtlas.AtlasRegion>) : Animation<TextureRegion>(frameDuration, regions) {

    var stateTime = 0f
    val currentFrame get() = getKeyFrame(stateTime)

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