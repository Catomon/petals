package ctmn.petals.playscreen.seqactions

class UpdateAction(val action: (delta: Float) -> Boolean) : SeqAction() {

    override fun update(deltaTime: Float) {
        if (!isDone) {
            isDone =  action(deltaTime)
        }
    }
}