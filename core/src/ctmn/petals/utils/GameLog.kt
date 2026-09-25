package ctmn.petals.utils

import com.badlogic.gdx.Gdx
import ctmn.petals.Config

/**
 * Central logging facade.
 *
 * Errors are always printed. The noisy categories are opt-in from the desktop launcher
 * arguments so normal runs stay readable:
 *
 * - `botlog`: bot balance lines used for tuning the AI (see
 *   [ctmn.petals.bot.v2.BotLog]).
 * - `verboselog`: engine chatter such as asset loading, the event logger, turn manager and
 *   GUI listener changes.
 */
object GameLog {

    const val BALANCE_TAG = "BotBalance"

    val balanceEnabled: Boolean get() = Config.botLog

    val verboseEnabled: Boolean get() = Config.verboseLog

    /** Bot balance line, enabled with the `botlog` run argument. */
    fun balance(message: String) {
        if (!balanceEnabled) return
        Gdx.app.log(BALANCE_TAG, message)
    }

    /** Engine chatter, enabled with the `verboselog` run argument. */
    fun verbose(tag: String, message: String) {
        if (!verboseEnabled) return
        Gdx.app.log(tag, message)
    }

    /** Always printed. */
    fun info(tag: String, message: String) {
        Gdx.app.log(tag, message)
    }

    /** Always printed. Use for real failures, never for chatter. */
    fun error(tag: String, message: String, throwable: Throwable? = null) {
        if (throwable != null) Gdx.app.error(tag, message, throwable)
        else Gdx.app.error(tag, message)
    }
}
