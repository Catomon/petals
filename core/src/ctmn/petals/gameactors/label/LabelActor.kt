package ctmn.petals.gameactors.label

import com.badlogic.gdx.scenes.scene2d.Actor
import com.badlogic.gdx.utils.ArrayMap

/** An actor that contains data and marks position like leader spawn or go-to-position etc. */
class LabelActor(
    val labelName: String? = null,
    val data: ArrayMap<String, String> = ArrayMap<String, String>(),
) : Actor() {

}