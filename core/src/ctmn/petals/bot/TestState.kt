package ctmn.petals.bot

interface TestState {
    fun enter() {}
    fun update(delta: Float) {}
    fun exit() {}
}