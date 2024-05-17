package ctmn.petals.effects

import ctmn.petals.Assets
import ctmn.petals.Const
import ctmn.petals.utils.RegionAnimation
import ctmn.petals.utils.setPositionByCenter
import com.badlogic.gdx.graphics.g2d.Batch
import com.badlogic.gdx.math.Vector2
import com.badlogic.gdx.scenes.scene2d.Stage
import com.badlogic.gdx.utils.Array
import ctmn.petals.newPlaySprite

class HealingEffect(private val vectors: Array<Vector2>, val assets: Assets) : ctmn.petals.effects.EffectActor() {
    init {
        animation = RegionAnimation(0.075f, assets.atlases.findRegions("effects/ability_healing"))
        lifeTime = animation!!.animationDuration

        sprite = newPlaySprite(animation!!.currentFrame)
    }

    override fun draw(batch: Batch, parentAlpha: Float) {
        super.draw(batch, parentAlpha)

        for (vector in vectors) {
            sprite.setPositionByCenter(vector.x + Const.TILE_SIZE / 2, vector.y + Const.TILE_SIZE / 2)
            sprite.draw(batch)
        }
    }

    override fun setStage(stage: Stage?) {
        super.setStage(stage)

        assets.getSound("heal_up.ogg").play()
    }
}
