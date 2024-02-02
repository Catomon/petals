package ctmn.petals.utils

open class State() {
    var update: (() -> Unit)? = null
    var onEnter: (() -> Unit)? = null
    var onExit: (() -> Unit)? = null
}