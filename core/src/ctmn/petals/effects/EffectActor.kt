package ctmn.petals.effects

import ctmn.petals.utils.RegionAnimation
import com.badlogic.gdx.graphics.g2d.Batch
import com.badlogic.gdx.graphics.g2d.Sprite
import com.badlogic.gdx.scenes.scene2d.Actor
import ctmn.petals.utils.setPositionByCenter

open class EffectActor : Actor() {

    var animation: RegionAnimation? = null
    var sprite = Sprite()

    var lifeTime = 0f

    val isDone get() = lifeTime <= 0

    override fun act(delta: Float) {
        super.act(delta)

        if (this.animation != null) {
            animation!!.update(delta)
            sprite.setRegion(this.animation!!.currentFrame)
        }

        lifeTime -= delta
        if (lifeTime <= 0)
            remove()
    }

    override fun draw(batch: Batch, parentAlpha: Float) {
        super.draw(batch, parentAlpha)

        sprite.draw(batch)
    }

    override fun positionChanged() {
        super.positionChanged()

        sprite.setPositionByCenter(x, y)
    }
}
