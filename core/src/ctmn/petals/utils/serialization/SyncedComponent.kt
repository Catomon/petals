package ctmn.petals.utils.serialization

import com.badlogic.ashley.core.Component

/**
 * Marker for components that should be written to and read from serialized game state
 * (currently used for multiplayer snapshots).
 *
 * Adding this marker is the single place that decides whether a component is synced, instead
 * of the old hand-maintained allow list / ignore list / per-class `when` blocks.
 */
interface SyncedComponent : Component
