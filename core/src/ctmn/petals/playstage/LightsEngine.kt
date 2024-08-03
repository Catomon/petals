package ctmn.petals.playstage

import com.badlogic.gdx.Gdx
import com.badlogic.gdx.graphics.Color
import com.badlogic.gdx.graphics.Pixmap
import com.badlogic.gdx.graphics.Texture
import com.badlogic.gdx.graphics.Texture.TextureFilter
import com.badlogic.gdx.graphics.g2d.Batch
import com.badlogic.gdx.graphics.g2d.Sprite
import com.badlogic.gdx.graphics.g2d.SpriteBatch
import com.badlogic.gdx.graphics.g2d.TextureRegion
import com.badlogic.gdx.graphics.glutils.FrameBuffer
import com.badlogic.gdx.math.Matrix4
import ctmn.petals.assets
import ctmn.petals.playscreen.selfName
import ctmn.petals.utils.centerX
import ctmn.petals.utils.centerY
import ctmn.petals.utils.setPositionByCenter

class LightsEngine(
    private val playStage: PlayStage,
    private val batch: Batch = SpriteBatch(),
) {

    private val ambientLightColor: Color
    private var lightsBuffer: FrameBuffer? = null
    private val lightsBufferRegion: TextureRegion

    private val blueLight: Sprite

    init {
        blueLight = Sprite(assets.findAtlasRegion("misc/blue_light"))
        ambientLightColor = Color(0.1f, 0.1f, 0.1f, 1.0f)
        lightsBufferRegion = TextureRegion()
    }

    fun draw(matrix4: Matrix4) {
        return

        batch.projectionMatrix = matrix4
        lightsBuffer!!.begin()
        Gdx.gl.glClearColor(
            ambientLightColor.r,
            ambientLightColor.g, ambientLightColor.b, ambientLightColor.a
        )
        Gdx.gl.glClear(16384)
        Gdx.gl20.glBlendFunc(770, 1)
        Gdx.gl20.glEnable(3042)
        batch.begin()
        batch.setBlendFunction(770, -1)
        ///

        for (tile in playStage.getTiles()) {
            if (tile.selfName.startsWith("pixie_nest")) {
                blueLight.setPositionByCenter(tile.centerX, tile.centerY)
                blueLight.draw(batch)
            }
        }

        ///
        batch.end()
        Gdx.gl.glBlendFunc(770, 1)
        lightsBuffer!!.end()
        batch.projectionMatrix.setToOrtho2D(
            0.0f,
            lightsBuffer!!.height.toFloat(),
            lightsBuffer!!.width.toFloat(), lightsBuffer!!.height.toFloat()
        )
        batch.begin()
        batch.setBlendFunction(774, 0)
        batch.draw(
            this.lightsBufferRegion, 0.0f,
            lightsBuffer!!.height.toFloat()
        )
        batch.end()
    }

    fun update(width: Int, height: Int) {
        if (this.lightsBuffer != null) {
            lightsBuffer!!.dispose()
        }

        this.lightsBuffer = FrameBuffer(Pixmap.Format.RGBA8888, width, height, false)
        (lightsBuffer!!.colorBufferTexture as Texture).setFilter(TextureFilter.Nearest, TextureFilter.Nearest)
        lightsBufferRegion.setRegion(lightsBuffer!!.colorBufferTexture as Texture)
        lightsBufferRegion.flip(false, true)
    }

    fun setAmbientLightColor(r: Float, g: Float, b: Float, a: Float) {
        ambientLightColor[r, g, b] = a
    }
}
