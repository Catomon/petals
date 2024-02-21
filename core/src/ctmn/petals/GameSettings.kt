package ctmn.petals

import com.badlogic.gdx.Gdx
import com.badlogic.gdx.Preferences
import ctmn.petals.utils.*
import java.lang.Exception
import java.util.*

val gameSettings = Gdx.app.getPreferences(GamePref.PREF_FILE_NAME)

fun GamePref.overridePrefs() {
    drawUnitAttackRange = true
}

fun GamePref.setEmptyToDefaultPrefs() {
    if (locale == null) locale = "en"
//    if (drawUnitAttackRange == null) drawUnitAttackRange = true
//    if (showAiGui == null) showAiGui = false

    save()
}

object GamePref {

    const val PREF_FILE_NAME = "saves.petals.ctmn"

    private const val LEVEL = "level"

    private const val LOCALE = "locale"
    private const val DRAW_UNIT_ATK_RANGE = "draw_unit_attack_range"
    private const val SHOW_AI_GUI = "show_ai_gui"

    val prefs: Preferences = Gdx.app.getPreferences(PREF_FILE_NAME)

    // always set random if it is not release build (for testing)
    val clientId: String =
        if (!Const.IS_RELEASE)
            UUID.randomUUID().toString()
        else
            if (gameSettings.contains("client_id"))
                gameSettings.getString("client_id")
            else
                UUID.randomUUID().toString().also { gameSettings.putString("client_id", it).flush() }

    var locale: String?
        get() = prefs.getString(LOCALE)
        set(value) { prefs.putString(LOCALE, value) }

    var drawUnitAttackRange: Boolean?
        get() = prefs.getBoolean(DRAW_UNIT_ATK_RANGE)
        set(value) {
            if (value == null) throw IllegalArgumentException()
            prefs.putBoolean(DRAW_UNIT_ATK_RANGE, value)
        }

    var showAiGui: Boolean?
        get() = prefs.getBoolean(SHOW_AI_GUI)
        set(value) {
            if (value == null) throw IllegalArgumentException()
            prefs.putBoolean(SHOW_AI_GUI, value)
        }

    var player: UserSave?
        get() {
            return try {
                fromGson(decryptData(prefs.getString("player"), generateSecretKey("cringe")), UserSave::class.java)
            } catch (e: Exception) {
                e.printStackTrace()
                null
            }
        }
        set(value) {
            if (value == null) throw IllegalArgumentException()
            prefs.putString("player", encryptData(value.toGson(), generateSecretKey("cringe")))
        }

    init {
        if (player == null) {
            player = UserSave("trash", 1, 2)
            prefs.flush()
        }

        println(prefs.getString("player"))
        println()
        println(decryptData(prefs.getString("player"), generateSecretKey("cringe")))
    }

    fun save()  {
        prefs.flush()
    }
}