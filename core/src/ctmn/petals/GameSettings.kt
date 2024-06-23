package ctmn.petals

import com.badlogic.gdx.Gdx
import com.badlogic.gdx.Preferences
import ctmn.petals.editor.IS_MOBILE
import ctmn.petals.utils.*
import java.lang.Exception
import java.util.*

fun GamePref.overridePrefs() {
    musicVolume = 0f
}

object GamePref {

    const val PREF_FILE_NAME = "settings.xml"

    private const val LEVEL = "level"

    private const val LOCALE = "locale"
    private const val DRAW_UNIT_ATK_RANGE = "draw_unit_attack_range"
    private const val SHOW_AI_GUI = "show_ai_gui"

    var prefs: Preferences = Gdx.app.getPreferences(PREF_FILE_NAME)
    val savesFolder =
        when {
            IS_MOBILE -> {
                Gdx.files.local("saves/")
            }

            Gdx.files.external(Const.LEGACY_USER_FOLDER + "/saves/").exists() -> {
                Gdx.files.external(Const.LEGACY_USER_FOLDER + "/saves/")
                    .apply { mkdirs() }
            }

            else -> {
                Gdx.files.external(Const.USER_FOLDER + "/saves/")
                    .apply { mkdirs() }
            }
        }


    // always set random if it is not release build (for testing)
    val clientId: String =
        if (!Const.IS_RELEASE)
            UUID.randomUUID().toString()
        else
            if (prefs.contains("client_id"))
                prefs.getString("client_id")
            else
                UUID.randomUUID().toString().also { prefs.putString("client_id", it).flush() }

    var locale: String
        get() = prefs.getString(LOCALE, GameLocale.ENGLISH)
        set(value) {
            prefs.putString(LOCALE, value)
        }

    var targetFps: Int
        get() = prefs.getInteger("target_fps", 60)
        set(value) {
            prefs.putInteger("target_fps", value)
        }

    var vSync: Boolean
        get() = prefs.getBoolean("vSync", true)
        set(value) {
            prefs.putBoolean("vSync", value)
        }

    var fullscreen: Boolean
        get() = prefs.getBoolean("fullscreen", true)
        set(value) {
            prefs.putBoolean("fullscreen", value)
        }

    var soundVolume: Float
        get() = prefs.getFloat("sound_volume", 0.50f)
        set(value) {
            prefs.putFloat("sound_volume", value)
        }

    var musicVolume: Float
        get() = prefs.getFloat("music_volume", 0f)
        set(value) {
            prefs.putFloat("music_volume", value)
        }

    var drawUnitAttackRange: Boolean
        get() = prefs.getBoolean(DRAW_UNIT_ATK_RANGE, true)
        set(value) {
            prefs.putBoolean(DRAW_UNIT_ATK_RANGE, value)
        }

    var autoEndTurn: Boolean
        get() = prefs.getBoolean("auto_end_turn", false)
        set(value) {
            prefs.putBoolean("auto_end_turn", value)
        }

    var showBotGui: Boolean
        get() = prefs.getBoolean(SHOW_AI_GUI, true) //todo
        set(value) {
            prefs.putBoolean(SHOW_AI_GUI, value)
        }

    var showTerrainBonus: Boolean
        get() = prefs.getBoolean("show_terrain_bonus", true)
        set(value) {
            prefs.putBoolean("show_terrain_bonus", value)
        }

    var player: UserSave
        get() {
            return try {
                fromGson(decryptData(prefs.getString("player"), generateSecretKey("cringe")), UserSave::class.java)
            } catch (e: Exception) {
                e.printStackTrace()
                UserSave()
            }
        }
        set(value) {
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

    fun save() {
        prefs.flush()
    }
}