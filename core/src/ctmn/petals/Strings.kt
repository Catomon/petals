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

fun getLangStringsByPrefs(): Strings {
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

    var general = General()
    var units = Units()

    class General {
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
        val close = "Close"
        val return_ = "Return"
        val confirm = "Confirm"
        val fullscreen = "Fullscreen"
        val show_attack_range_border = "Show attack range\nborder"
        val auto_end_turn = "Auto End Turn"
        val show_terrain_bonus = "Show terrain bonus"
        val target_fps = "Target fps"
        val music = "Music"
        val sound = "Sound"
        val language = "Language"
        val game_menu = "Game Menu"
        val continue_ = "Continue"
        val restart = "Restart"
        val end_game = "End game"
        val fog_of_war = "Fog Of War"
        val daytime = "Daytime"
        val season = "Season"
        val day = "day"
        val night = "night"
        val your_turn = "YOUR TURN"
        val enemy_turn = "ENEMY TURN"
        val ally_turn = "ALLY TURN"
        val buy_menu = "Base Menu"
        val player_slot = "Player Slot"
        val move_here = "Move here"
        val add_easy_bot = "Add Easy Bot"
        val player = "Player"
        val add_player = "Add Player"
        val set_goblin = "Set Goblin"
        val set_faerie = "Set Faerie"
        val remove = "Remove"
        val cancel = "Cancel"
        val same_screen = "Same Screen"
        val pass_and_play = "Pass and Play"
        val local_multiplayer = "Local Multiplayer"
        val host = "Host"
        val connecting = "Connecting..."
        val looking_for_server = "Looking for server..."
        val check_your_network_connection = "Check your network connection."
        val connect = "Connect"
        val no_local_servers_found = "No local servers found"
    }

    class Units {
        val seraphina = "Seraphina"
    }
}
