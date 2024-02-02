package ctmn.petals

import com.badlogic.gdx.Gdx
import com.google.gson.Gson

var strings: Strings = getLangStringsByPrefs()

fun getLangStringsByPrefs() : Strings {
    val file = Gdx.files.internal("lang/${GamePref.locale}.json")
    if (!file.exists()) {
        check(GamePref.locale != "en")

        GamePref.locale = "en"
        GamePref.save()
        return getLangStringsByPrefs()
    }

    return Gson().fromJson(file.readString(Charsets.UTF_8.name()), Strings::class.java)
}

class Strings {

    var menu = Menu()
    var play = Play()
    var units = Units()

    class Menu {
        val story = "Story"
        val match = "Match"
        val editor = "Editor"
        val settings = "Settings"
        val exit = "Exit"
    }

    class Play {
        val your_turn = "YOUR TURN"
        val enemy_turn = "ENEMY TURN"
        val ally_turn = "ALLY TURN"
    }

    class Units {
        val alissa = "Alissa"
    }
}
