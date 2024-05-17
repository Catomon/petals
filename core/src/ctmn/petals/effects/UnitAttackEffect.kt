package ctmn.petals.effects

import ctmn.petals.Assets
import ctmn.petals.Const
import ctmn.petals.utils.RegionAnimation
import com.badlogic.gdx.scenes.scene2d.Stage
import ctmn.petals.newPlaySprite

class UnitAttackEffect(val assets: Assets) : ctmn.petals.effects.EffectActor() {
    init {
        animation = RegionAnimation(0.015f, assets.atlases.findRegions("effects/units_attack"))
        sprite = newPlaySprite(animation!!.currentFrame)
        lifeTime = animation!!.animationDuration
        if (sprite.width < Const.TILE_SIZE)
            sprite.setSize(Const.TILE_SIZE.toFloat(), Const.TILE_SIZE.toFloat())
    }

    override fun setStage(stage: Stage?) {
        super.setStage(stage)

        if (stage != null)
            assets.getSound("hit.ogg").play()
    }
}
