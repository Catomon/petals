package ctmn.petals.playscreen.seqactions

class OneSeqAction(val action: () -> Unit) : SeqAction() {

    override fun update(deltaTime: Float) {
        if (!isDone) {
            action()
            isDone = true
        }
    }
}