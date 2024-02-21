package ctmn.petals.effects

import com.badlogic.gdx.graphics.g2d.Batch
import com.badlogic.gdx.scenes.scene2d.actions.Actions
import com.badlogic.gdx.scenes.scene2d.actions.SequenceAction
import ctmn.petals.Const
import ctmn.petals.actors.actions.UpdateAction
import ctmn.petals.assets
import ctmn.petals.newPlaySprite
import ctmn.petals.utils.*
import kotlin.math.abs

class FireballEffect(startX: Float, startY: Float, endX: Float, endY: Float) : EffectActor() {

    private val fireballAnimation = RegionAnimation(0.25f, assets.findAtlasRegions("effects/fireball"))
    private val fireballSprite = newPlaySprite(fireballAnimation.currentFrame)

    private val movement = Movement().apply {
        deceleration.x = 0f
        deceleration.y = 0f
        maxVelocity.x = 500f
        maxVelocity.y = 500f
        velocity.x = 150f
        velocity.y = 150f
    }

    private var distance = abs(startX - endX) + abs(startY - endY)

    init {
        setPosition(startX, startY)

        movement.setDirection(startX, startY, endX, endY)

        fireballSprite.rotation = calculateMoveDirection(startX, startY, endX, endY)

        addAction(
            SequenceAction(
                UpdateAction { delta ->
                    movement.update(delta)
                    x += movement.dirVelocity.x
                    y += movement.dirVelocity.y

                    val newDist = abs(x - endX) + abs(y - endY)
                    if (distance <= newDist) {
                        true
                    } else {
                        distance = newDist

                        false
                    }
                },
                Actions.run {
                    val explosion = FlameAbilityEffect(0, endX.tiled(), endY.tiled(), assets)
                    stage.addActor(explosion)
                    val particles = ExplosionParticles()
                    particles.setPosition(endX, endY - Const.TILE_SIZE / 2)
                    stage.addActor(particles)

                    lifeTime = 0f
                },
                Actions.removeActor()
            )
        )

        lifeTime = 5f
    }

    override fun act(delta: Float) {
        super.act(delta)

        fireballAnimation.update(delta)
    }

    override fun draw(batch: Batch, parentAlpha: Float) {
        //super.draw(batch, parentAlpha)

        fireballSprite.setRegion(fireballAnimation.currentFrame)
        fireballSprite.setPositionByCenter(x, y)
        fireballSprite.draw(batch)
    }
}