package ctmn.petals.map.label

import com.badlogic.gdx.scenes.scene2d.Actor
import com.badlogic.gdx.utils.ArrayMap
import ctmn.petals.utils.unTiled

object MapLabels {
    // should contain 'id'(0-7) and 'player_id'(1-8) int values; marks player initial position, claims primary base under it.
    const val PLAYER = "player"
}

/** An actor that contains data and marks position like leader spawn or go-to-position etc.
 * label.name != labelName */
class LabelActor(
    val labelName: String,
    val data: ArrayMap<String, String> = ArrayMap<String, String>(),
) : Actor() {

    constructor(labelName: String, tileX: Int, tileY: Int) : this(labelName) {
        setPosition(tileX, tileY)
    }

    fun setPosition(x: Int, y: Int) {
        setPosition(x.unTiled(), y.unTiled())
    }
}