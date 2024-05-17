package ctmn.petals.effects

import ctmn.petals.Assets
import ctmn.petals.Const
import ctmn.petals.utils.RegionAnimation
import com.badlogic.gdx.scenes.scene2d.Stage
import ctmn.petals.newPlaySprite

class UnitDeathExplosionEffect(val assets: Assets) : EffectActor() {
    init {
        animation = RegionAnimation(0.05f, assets.atlases.findRegions("effects/unit_death"))
        sprite = newPlaySprite(animation!!.currentFrame)
        lifeTime = animation!!.animationDuration
        if (sprite.width < Const.TILE_SIZE)
            sprite.setSize(Const.TILE_SIZE.toFloat(), Const.TILE_SIZE.toFloat())
    }

    override fun setStage(stage: Stage?) {
        super.setStage(stage)

        if (stage != null)
            assets.getSound("unit_explosion.ogg").play()
    }

    override fun act(delta: Float) {
        super.act(delta)

        //called twice
                //animation?.update(delta)
    }
}
