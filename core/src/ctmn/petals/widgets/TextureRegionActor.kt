package ctmn.petals.widgets

import com.badlogic.gdx.graphics.g2d.Batch
import com.badlogic.gdx.graphics.g2d.TextureRegion
import com.badlogic.gdx.scenes.scene2d.Actor

class TextureRegionActor(val textureRegion: TextureRegion? = null) : Actor() {

    init {
        if (textureRegion != null) {
            setSize(textureRegion.regionWidth.toFloat(), textureRegion.regionHeight.toFloat())
        }
    }

    override fun draw(batch: Batch, parentAlpha: Float) {
        batch.draw(textureRegion ?: return, x, y, width, height)
    }
}