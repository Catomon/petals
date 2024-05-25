package ctmn.petals.effects

import com.badlogic.gdx.graphics.g2d.Animation
import com.badlogic.gdx.graphics.g2d.Batch
import com.badlogic.gdx.scenes.scene2d.Actor
import com.badlogic.gdx.scenes.scene2d.Stage
import com.badlogic.gdx.scenes.scene2d.actions.DelayAction
import com.badlogic.gdx.scenes.scene2d.actions.RemoveAction
import ctmn.petals.actors.actions.JumpAction
import ctmn.petals.actors.actions.OneAction
import ctmn.petals.assets
import ctmn.petals.utils.AnimatedSprite
import ctmn.petals.utils.err
import ctmn.petals.utils.setPositionByCenter

class MissileActor(
    missileName: String = "catapult_missile",
    explosionName: String = "catapult_explosion",
    targetX: Float = 0f,
    targetY: Float = 0f,
) : Actor() {

    private val missileSprite = AnimatedSprite(assets.effectsAtlas.findRegions(missileName))
    private val explosionSprite = AnimatedSprite(assets.effectsAtlas.findRegions(explosionName), 0.25f).apply {
        animation.playMode = Animation.PlayMode.NORMAL
    }
    private var currentSprite = missileSprite
    private val jumpAction = JumpAction(targetX, targetY)

    var isLanded = false

    override fun setStage(stage: Stage?) {
        super.setStage(stage)

        if (stage != null) {
            addAction(jumpAction)
            addAction(DelayAction(5f).apply { action = OneAction {
                err("Effect time out ${this@MissileActor}")
                addAction(RemoveAction())
            } })
        }
    }

    fun setStart(x: Float, y: Float) {
        setPosition(x, y)
        jumpAction.startX = x
        jumpAction.startY = y
        jumpAction.setStart(x, y)
    }

    fun setTarget(x: Float, y: Float) {
        jumpAction.endX = x
        jumpAction.endY = y
    }

    override fun act(delta: Float) {
        super.act(delta)

        currentSprite.update(delta)

        if (jumpAction.actor == null) {
            isLanded = true
            currentSprite = explosionSprite
        }

        if (explosionSprite.animation.isAnimationFinished(explosionSprite.animation.stateTime))
            remove()
    }

    override fun draw(batch: Batch, parentAlpha: Float) {
        super.draw(batch, parentAlpha)

        currentSprite.setPositionByCenter(x, y)
        currentSprite.draw(batch)
    }
}
