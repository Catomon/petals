package ctmn.petals.actors.actions

import com.badlogic.gdx.scenes.scene2d.Action

class RepeatAction(private val delay: Float, private var amount: Int, val action: (delta: Float) -> Boolean) : Action() {

    var isDone = false
        private set
    private var time = delay

    override fun act(delta: Float): Boolean {
        if (!isDone) {
            time += delta

            if (time >= delay) {
                isDone = action.invoke(time) || amount <= 1
                time = 0f
                amount--
            }
        }

        return isDone
    }
}