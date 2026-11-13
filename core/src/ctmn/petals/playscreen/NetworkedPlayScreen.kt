package ctmn.petals.playscreen

/**
 * A battle that is backed by a server. The play screen only knows this much about it, so the
 * local game never has to link against the networking stack.
 */
interface NetworkedPlayScreen {
    /** True on a client, which is the only side that can pull a fresh state. */
    val isNetworkClient: Boolean

    fun requestRemoteState()

    fun leaveNetwork()
}
