package ctmn.petals.story

import ctmn.petals.Const.FAE_STORY_ID
import ctmn.petals.story.alissa.AlissaStorySave
import ctmn.petals.story.alissa.scenarios.*

class FaeStory : Story(
    "Fae Foe",
    FAE_STORY_ID,
    AlissaStorySave().apply { save_name = "fae_foe"; story_id = FAE_STORY_ID }
) {

    override fun initScenarios() {
        super.initScenarios()

        check(storySave is AlissaStorySave)

        scenarios.add(Scenario1())
    }
}