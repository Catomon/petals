package ctmn.petals.bot.v2

/**
 * Tunes the same [AiBot] brain. Higher difficulty reacts faster, makes better trades,
 * uses abilities/buildings and expands its economy more aggressively.
 */
enum class Difficulty(
    val displayName: String,
    /** seconds to wait between issuing actions */
    val actionDelay: Float,
    /** seconds with no available action before ending the turn */
    val endTurnDelay: Float,
    /** 0..1; used for risk tolerance and focus fire */
    val skill: Float,
    val useAbilities: Boolean,
    val useBuildings: Boolean,
    val expandBases: Boolean,
    /** how many workers the bot tries to keep alive */
    val maxWorkers: Int,
    /** 0..1; how far units are willing to push without support */
    val aggression: Float,
) {
    EASY(
        displayName = "Easy",
        actionDelay = 0.9f,
        endTurnDelay = 1.5f,
        skill = 0.35f,
        useAbilities = false,
        useBuildings = false,
        expandBases = false,
        maxWorkers = 1,
        aggression = 0.4f,
    ),
    NORMAL(
        displayName = "Normal",
        actionDelay = 0.5f,
        endTurnDelay = 1.0f,
        skill = 0.65f,
        useAbilities = true,
        useBuildings = true,
        expandBases = true,
        maxWorkers = 2,
        aggression = 0.7f,
    ),
    HARD(
        displayName = "Hard",
        actionDelay = 0.3f,
        endTurnDelay = 0.7f,
        skill = 1.0f,
        useAbilities = true,
        useBuildings = true,
        expandBases = true,
        maxWorkers = 3,
        aggression = 1.0f,
    ),
}
