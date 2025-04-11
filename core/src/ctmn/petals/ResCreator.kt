package ctmn.petals

import com.badlogic.gdx.graphics.g2d.Sprite
import com.badlogic.gdx.graphics.g2d.TextureRegion
import ctmn.petals.utils.AnimatedSprite

const val PLAY_SPRITE_SCALE = 1f
const val GUI_SPRITE_SCALE = 3f

fun Int.toPlayScale() = (this * PLAY_SPRITE_SCALE).toInt()
fun Float.toPlayScale() = this * PLAY_SPRITE_SCALE

fun newPlaySprite(region: TextureRegion): Sprite {
    return Sprite(region).apply { setSize(width / PLAY_SPRITE_SCALE, height / PLAY_SPRITE_SCALE); setOriginCenter() }
}

fun newPlayPuiSprite(region: TextureRegion): Sprite {
    return Sprite(region).apply { setSize(width / GUI_SPRITE_SCALE, height / GUI_SPRITE_SCALE); setOriginCenter() }
}

fun AnimatedSprite.resizeFromPui() =
    this.apply { setSize(width / GUI_SPRITE_SCALE, height / GUI_SPRITE_SCALE); setOriginCenter() }