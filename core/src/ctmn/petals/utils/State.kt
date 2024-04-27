package ctmn.petals.utils

open class State(val name: String = "unnamed") {
    var update: (() -> Unit)? = null
    var onEnter: (() -> Unit)? = null
    var onExit: (() -> Unit)? = null
}