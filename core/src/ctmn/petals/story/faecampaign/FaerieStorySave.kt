package ctmn.petals.story.faecampaign

import ctmn.petals.Config
import ctmn.petals.story.StorySaveGson
import com.google.gson.annotations.SerializedName
import ctmn.petals.story.alissa.AlissaSave
import ctmn.petals.story.alissa.CharacterSave

class FaerieStorySave : StorySaveGson(
    "fairy_campaign",
    Config.FAERIE_CAMPAIGN_ID) {

    @SerializedName("alissa")
    val alissa = AlissaSave()

    val characters = mutableListOf<CharacterSave>()
}