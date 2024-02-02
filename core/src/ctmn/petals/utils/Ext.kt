package ctmn.petals.utils

fun <T> T.alsoIf(cond: Boolean, doIt: (T) -> Unit) : T {
    if (cond) doIt(this)

    return this
}