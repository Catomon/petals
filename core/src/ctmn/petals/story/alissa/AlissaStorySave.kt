package ctmn.petals.story.alissa

import ctmn.petals.GameConst
import ctmn.petals.story.StorySaveGson
import com.google.gson.annotations.SerializedName

class AlissaStorySave : StorySaveGson(
    "alissa_save",
    GameConst.TREASURED_PETALS_STORY_ID) {

    @SerializedName("alissa")
    val alissa = AlissaSave()

    val characters = mutableListOf<CharacterSave>()
}