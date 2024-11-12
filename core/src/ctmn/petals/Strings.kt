package ctmn.petals

import com.badlogic.gdx.Gdx
import com.google.gson.Gson

var strings: Strings = getLangStringsByPrefs()

object GameLocale {
    const val ENGLISH = "en";
    const val JAPANESE = "jp"
    const val SPANISH = "es"
    const val RUSSIAN = "ru"
}

fun getLangStringsByPrefs() : Strings {
    val file = Gdx.files.internal("lang/${GamePref.locale}.json")
    if (!file.exists()) {
        check(GamePref.locale != GameLocale.ENGLISH)

        GamePref.locale = GameLocale.ENGLISH
        GamePref.save()
        return getLangStringsByPrefs()
    }

    return Gson().fromJson(file.readString(Charsets.UTF_8.name()), Strings::class.java)
}

class Strings {

    var ui = UI()
    var play = Play()
    var units = Units()

    class UI {
        val story = "Story"
        val levels = "Campaign"
        val quickplay = "Quickplay"
        val match = "Match"
        val vsBot = "Vs Bot"
        val vsPlayer = "Vs Player"
        val profile = "Profile"
        val editor = "Editor"
        val settings = "Settings"
        val exit = "Exit"
        val close = "close"
    }

    class Play {
        val your_turn = "YOUR TURN"
        val enemy_turn = "ENEMY TURN"
        val ally_turn = "ALLY TURN"
        val buy_menu = "Base Menu"
    }

    class Units {
        val seraphina = "Seraphina"
    }
}
