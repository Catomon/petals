package ctmn.petals.bot.v2

import com.badlogic.gdx.utils.Array
import ctmn.petals.unit.UnitActor

/**
 * Per-instance configuration. Difficulty covers the general behaviour, while these knobs
 * let campaign levels keep their scripted flavours (custom unit rosters, roaming enemies).
 */
data class AiBotSettings(
    val difficulty: Difficulty = Difficulty.NORMAL,
    /**
     * When non-null, overrides the species roster. Campaign levels use custom creatures
     * that are not part of [ctmn.petals.player.Species].
     */
    val speciesUnits: Array<UnitActor>? = null,
    /** When true, the economy is never used even if the map offers resources. */
    val forceCombatOnly: Boolean = false,
    /** Legacy roaming knobs, kept so scripted levels keep their flavour. */
    val permaAgro: Boolean = true,
    val agroRange: Int = 6,
    val roamingIfNoAgro: Boolean = false,
    val roamingMaxRange: Int = 3,
)
