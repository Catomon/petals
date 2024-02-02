package ctmn.petals.actors.actions

import com.badlogic.gdx.scenes.scene2d.Action

class OneAction(val action: () -> Unit) : Action() {

    var isDone = false

    override fun act(delta: Float): Boolean {
        if (!isDone) {
            action.invoke()

            isDone = true
        }

        return true
    }
}