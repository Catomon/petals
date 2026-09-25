package ctmn.petals.bot.v2

/**
 * Tunes the same [AiBot] brain. Difficulty does not decide which units the bot is allowed to
 * want: every difficulty runs the same planner and composition rules, so the army it fields
 * is a consequence of how well it plays the economy.
 *
 * - [skill] controls decision quality. Lower skill means the bot more often picks a runner-up
 *   action instead of the best one it can see.
 * - [ambition] scales how large an army and economy the bot aims for.
 * - [aggression] is how long wounded units keep pushing without support.
 * - [useAbilities], [useBuildings] and [expandBases] gate whole systems for [EASY].
 * - [incomeBonus] is a deliberate cheat reserved for [INSANE].
 */
enum class Difficulty(
    val displayName: String,
    /** seconds to wait between issuing actions */
    val actionDelay: Float,
    /** seconds with no available action before ending the turn */
    val endTurnDelay: Float,
    /** 0..1 decision quality: 1 means it always picks the best action it can see */
    val skill: Float,
    /** 0..1 scales army size targets and economy ambition */
    val ambition: Float,
    /** 0..1 how far wounded units keep pushing without support */
    val aggression: Float,
    val useAbilities: Boolean,
    val useBuildings: Boolean,
    val expandBases: Boolean,
    /** Extra credits granted at the start of each of this bot's turns. INSANE only. */
    val incomeBonus: Int = 0,
) {
    EASY(
        displayName = "Easy",
        actionDelay = 0.9f,
        endTurnDelay = 1.5f,
        skill = 0.3f,
        ambition = 0.4f,
        aggression = 0.4f,
        useAbilities = false,
        useBuildings = false,
        expandBases = false,
    ),
    NORMAL(
        displayName = "Normal",
        actionDelay = 0.5f,
        endTurnDelay = 1.0f,
        skill = 0.7f,
        ambition = 0.7f,
        aggression = 0.7f,
        useAbilities = true,
        useBuildings = true,
        expandBases = true,
    ),
    HARD(
        displayName = "Hard",
        actionDelay = 0.3f,
        endTurnDelay = 0.7f,
        skill = 1.0f,
        ambition = 1.0f,
        aggression = 1.0f,
        useAbilities = true,
        useBuildings = true,
        expandBases = true,
    ),
    INSANE(
        displayName = "Insane",
        actionDelay = 0.2f,
        endTurnDelay = 0.5f,
        skill = 1.0f,
        ambition = 1.15f,
        aggression = 1.1f,
        useAbilities = true,
        useBuildings = true,
        expandBases = true,
        incomeBonus = 150,
    ),
}
