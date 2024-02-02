package ctmn.petals.actors.actions

import com.badlogic.gdx.scenes.scene2d.Action

class TimeAction(val action: (time: Float) -> Boolean) : Action() {

    var isDone = false
    var time = 0f

    override fun act(delta: Float): Boolean {
        if (!isDone) {
            time += delta
            isDone = action.invoke(time)
        }

        return isDone
    }
}