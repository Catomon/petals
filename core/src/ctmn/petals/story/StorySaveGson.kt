package ctmn.petals.story

import ctmn.petals.Const
import com.badlogic.gdx.math.MathUtils

open class StorySaveGson(
    var save_name: String = "New_Save${MathUtils.random(0, 100)}",
    var story_id: Int
) {

    var game_version = Const.APP_VER_NAME

    var friendly_fire = false

    var progress = Progress()

    val extra = mutableMapOf<String, String>()

    override fun toString(): String {
        return "StorySave name: $save_name, progress: $progress, v:$game_version"
    }
}
