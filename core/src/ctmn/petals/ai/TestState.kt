package ctmn.petals.ai

interface TestState {
    fun enter() {}
    fun update(delta: Float) {}
    fun exit() {}
}