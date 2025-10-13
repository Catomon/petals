package ctmn.petals.effects

import ctmn.petals.Assets
import ctmn.petals.Config
import ctmn.petals.utils.RegionAnimation
import com.badlogic.gdx.scenes.scene2d.Stage
import ctmn.petals.newPlaySprite

class UnitAttackEffect(val assets: Assets) : ctmn.petals.effects.EffectActor() {
    init {
        animation = RegionAnimation(0.015f, assets.atlases.findRegions("effects/units_attack"))
        sprite = newPlaySprite(animation!!.currentFrame)
        lifeTime = animation!!.animationDuration
        if (sprite.width < Config.TILE_SIZE)
            sprite.setSize(Config.TILE_SIZE.toFloat(), Config.TILE_SIZE.toFloat())
    }

    override fun setStage(stage: Stage?) {
        super.setStage(stage)

//        if (stage != null)
//            assets.getSound("hit2.ogg").play()
    }
}
