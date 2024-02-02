package ctmn.petals.effects

import com.badlogic.gdx.Gdx
import com.badlogic.gdx.graphics.g2d.Batch
import com.badlogic.gdx.graphics.g2d.ParticleEffect
import com.badlogic.gdx.scenes.scene2d.Actor
import com.badlogic.gdx.scenes.scene2d.Stage

class ExplosionParticles : Actor() {

    private val particleEffect = ParticleEffect()

    init {
        // particle , images dir
        particleEffect.load(Gdx.files.internal("particles/meteorite.p"), Gdx.files.internal("particles"))
        particleEffect.start()

        particleEffect.scaleEffect(1f)
    }

    override fun setStage(stage: Stage?) {
        super.setStage(stage)

        if (stage == null) {
            particleEffect.dispose()
        }
    }

    override fun act(delta: Float) {
        super.act(delta)

        particleEffect.update(delta)

        if (particleEffect.isComplete)
            remove()
    }

    override fun draw(batch: Batch, parentAlpha: Float) {
        super.draw(batch, parentAlpha)

        particleEffect.draw(batch)
    }

    override fun positionChanged() {
        super.positionChanged()

        particleEffect.setPosition(x, y)
    }
}
