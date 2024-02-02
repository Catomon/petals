package ctmn.petals

import com.badlogic.gdx.Gdx
import com.badlogic.gdx.Preferences
import java.util.*

val gameSettings = Gdx.app.getPreferences("ctmn.petals.settings")

object GamePref {

    private const val LOCALE = "locale"
    private const val DRAW_UNIT_ATK_RANGE = "draw_unit_attack_range"

    val prefs: Preferences = Gdx.app.getPreferences("settings")

    // always set random if it is not release build (for testing)
    val clientId: String =
        if (!GameConst.IS_RELEASE)
            UUID.randomUUID().toString()
        else
            if (gameSettings.contains("client_id"))
                gameSettings.getString("client_id")
            else
                UUID.randomUUID().toString().also { gameSettings.putString("client_id", it).flush() }

    var locale: String
        get() = prefs.getString(LOCALE)
        set(value) { prefs.putString(LOCALE, value) }

    var drawUnitAttackRange: Boolean?
        get() = prefs.getBoolean(DRAW_UNIT_ATK_RANGE)
        set(value) {
            if (value == null) throw IllegalArgumentException()
            prefs.putBoolean(DRAW_UNIT_ATK_RANGE, value)
        }

    init {

    }

    fun save()  {
        prefs.flush()
    }
}