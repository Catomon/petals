package ctmn.petals.sim

/**
 * A [Battle] backed by a headless [SimField]. This is the simulation the dedicated server
 * runs for every match, with no scene2d stage, no GUI and no GL context.
 */
class ServerBattle : Battle() {
    init {
        attachField(SimField())
    }
}
