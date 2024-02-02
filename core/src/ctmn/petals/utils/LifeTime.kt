package ctmn.petals.utils

class LifeTime(var time: Float, val onExpired: (() -> Unit)?) {

    var isDone = false

    fun update(delta: Float) {
        time -= delta
        if (!isDone && time <= 0) {
            onExpired?.invoke()
            isDone = true
        }
    }
}