package ctmn.petals

/**
 * Work that needs the networking stack. Launchers that have it install these, so the shared
 * screens never reference it directly.
 */
object PlatformHooks {
    var onDispose: (() -> Unit)? = null
    var startOnlineAutoJoin: ((PetalsGame, String, Int) -> Unit)? = null
}
