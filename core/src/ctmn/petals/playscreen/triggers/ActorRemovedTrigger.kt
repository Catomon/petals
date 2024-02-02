package ctmn.petals.playscreen.triggers

import com.badlogic.gdx.scenes.scene2d.Actor

class ActorRemovedTrigger(val actor: Actor) : Trigger() {

    override fun check(delta: Float): Boolean = actor.stage == null
}