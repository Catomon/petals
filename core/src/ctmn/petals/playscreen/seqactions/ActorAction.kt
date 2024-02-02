package ctmn.petals.playscreen.seqactions

import ctmn.petals.playscreen.PlayScreen
import com.badlogic.gdx.scenes.scene2d.Action
import com.badlogic.gdx.scenes.scene2d.Actor

class ActorAction(val actor: Actor, val act: Action) : SeqAction() {

    override fun update(deltaTime: Float) {
        isDone = act.actor == null
    }

    override fun onStart(playScreen: PlayScreen): Boolean {
        if (act.actor != actor)
            actor.addAction(act)

        return true
    }
}