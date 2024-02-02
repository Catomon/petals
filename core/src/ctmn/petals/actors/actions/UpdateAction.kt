package ctmn.petals.actors.actions

import com.badlogic.gdx.scenes.scene2d.Action

class UpdateAction(val action: (delta: Float) -> Boolean) : Action() {

    var isDone = false

    override fun act(delta: Float): Boolean {
        if (!isDone) {
            isDone = action.invoke(delta)
        }

        return isDone
    }
}