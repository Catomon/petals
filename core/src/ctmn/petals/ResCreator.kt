package ctmn.petals

import com.badlogic.gdx.graphics.g2d.Sprite
import com.badlogic.gdx.graphics.g2d.TextureRegion

// creates a sprite and makes it 4 times smaller
fun newPlaySprite(region: TextureRegion) : Sprite {
    return Sprite(region).apply { setSize(width / 4, height / 4); setOriginCenter() }
}