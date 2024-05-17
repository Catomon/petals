package ctmn.petals.effects

import com.badlogic.gdx.graphics.OrthographicCamera
import com.badlogic.gdx.scenes.scene2d.Stage
import ctmn.petals.Const
import ctmn.petals.actors.actions.CameraShakeAction
import ctmn.petals.assets
import ctmn.petals.newPlaySprite
import ctmn.petals.playscreen.playStageOrNull
import ctmn.petals.utils.Movement
import ctmn.petals.utils.RegionAnimation
import ctmn.petals.utils.setPosByCenter
import ctmn.petals.utils.tiled

class MeteoriteEffect : EffectActor() {

    private var endX = 0f
    private var endY = 0f

    private val movement = Movement().apply {
        deceleration.x = 1500f
        deceleration.y = 1500f
        maxVelocity.x = 1000f
        maxVelocity.y = 1000f
    }

    private val dAcceleration = 2000f

    private var isExploded = false

    init {
        animation = RegionAnimation(0.25f, assets.atlases.findRegions("effects/meteorite"))
        sprite = newPlaySprite(animation!!.currentFrame)

        lifeTime = 10f
    }

    override fun act(delta: Float) {
        movement.acceleration.set(dAcceleration, dAcceleration)
        //movement.acceleration.y = -dAcceleration
        movement.update(delta)

        x += movement.dirVelocity.x
        y += movement.dirVelocity.y

        if (y <= endY - sprite.height / 3 && !isExploded) {
            val explosion = FlameAbilityEffect(1, endX.tiled(), endY.tiled(), assets)
            stage.addActor(explosion)
            val particles = ExplosionParticles()
            particles.setPosition(endX, endY - Const.TILE_SIZE / 2)
            stage.addActor(particles)

            lifeTime = 0f

            playStageOrNull?.addAction(CameraShakeAction())

            isExploded = true
        }

        super.act(delta)
    }

    override fun setStage(stage: Stage?) {
        super.setStage(stage)

        if (stage != null) {
            endX = x
            endY = y
            setPosByCenter(endX + 32f,
                endY + stage.viewport.screenY + (stage.viewport.worldHeight)
                        * (stage.viewport.camera as OrthographicCamera).zoom + 50f)

            movement.setDirection(x, y, endX, endY)
        }
    }
}