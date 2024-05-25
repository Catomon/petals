package ctmn.petals.story.faecampaign

import ctmn.petals.Const
import ctmn.petals.story.Story
import ctmn.petals.story.faecampaign.levels.*

class FaerieStory(storySave: FaerieStorySave = FaerieStorySave()) : Story(
    "Levels",
    Const.FAERIE_CAMPAIGN_ID,
    storySave
) {

    override fun addScenarios() {
        super.addScenarios()

        check(storySave is FaerieStorySave)

        scenarios["lv_1"] = Level1::class.java
        scenarios["lv_2"] = Level2::class.java
        scenarios["lv_3"] = Level3::class.java
        scenarios["lv_4"] = Level4::class.java
        scenarios["lv_5"] = Level5::class.java
        scenarios["lv_6"] = Level6::class.java
        scenarios["lv_7"] = Level7::class.java
        scenarios["lv_8"] = Level8::class.java
        scenarios["lv_9"] = Level9::class.java
        scenarios["lv_10"] = Level10::class.java
        scenarios["lv_11"] = Level11::class.java
        scenarios["lv_12"] = Level12::class.java
        scenarios["lv_13"] = Level13::class.java
//        scenarios["lv_14"] = Level14::class.java
//        scenarios["lv_15"] = Level15::class.java
//        scenarios["lv_16"] = Level16::class.java
        //        scenarios["lv_17"] = Level16::class.java
        //        scenarios["lv_18"] = Level16::class.java
        //        scenarios["lv_19"] = Level16::class.java
        //        scenarios["lv_20"] = Level16::class.java
    }
}