package ctmn.petals.bot.v2.actions

import ctmn.petals.bot.v2.AiBot

/**
 * One atomic decision. The bot builds a list of these every tick, evaluates them and
 * executes the highest scoring one. [evaluate] must be side effect free.
 */
abstract class BotAction(protected val bot: AiBot) {

    abstract fun evaluate(): Int

    abstract fun execute(): Boolean

    /** Used to avoid retrying an action that keeps failing within the same turn. */
    open fun signature(): String = this::class.simpleName ?: "action"

    /**
     * What kind of command [execute] actually issued. Actions that can either do their job
     * or just step towards it (capture, destroy, build) override this, so the balance log
     * does not count approach moves as real actions.
     */
    open fun commandKind(): String = signature().substringBefore('@').substringBefore(':')

    companion object {
        const val IMPOSSIBLE = 0
    }
}
