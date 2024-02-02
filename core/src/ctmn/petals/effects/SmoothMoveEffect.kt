package ctmn.petals.effects

import com.badlogic.gdx.math.Interpolation
import com.badlogic.gdx.scenes.scene2d.actions.Actions
import com.badlogic.gdx.scenes.scene2d.actions.MoveToAction
import com.badlogic.gdx.scenes.scene2d.actions.SequenceAction
import ctmn.petals.assets
import ctmn.petals.newPlaySprite

class SmoothMoveEffect(
    private val endX: Float,
    private val duration: Float) : EffectActor() {

    private val moveAction: MoveToAction = MoveToAction()

    init {
        y = 100f

        sprite = newPlaySprite(assets.findAtlasRegion("effects/fireball"))

        moveAction.duration = duration
        moveAction.setPosition(endX, y)
        moveAction.interpolation = Interpolation.slowFast

        val sequenceAction = SequenceAction()
        sequenceAction.addAction(moveAction)
        sequenceAction.addAction(Actions.removeActor())

        addAction(sequenceAction)

        lifeTime = 10f
    }
}