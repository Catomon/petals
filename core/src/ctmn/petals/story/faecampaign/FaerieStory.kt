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
        scenarios["lv_1a"] = Level1A::class.java
        scenarios["lv_2"] = Level2::class.java
        scenarios["lv_3"] = Level3::class.java
        scenarios["lv_4"] = Level4::class.java
        scenarios["lv_5"] = Level5::class.java
    }
}