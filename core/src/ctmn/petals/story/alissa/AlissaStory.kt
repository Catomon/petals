package ctmn.petals.story.alissa

import ctmn.petals.Const.TREASURED_PETALS_STORY_ID
import ctmn.petals.story.Story

class AlissaStory : Story(
    "The Treasured Petals",
    TREASURED_PETALS_STORY_ID,
    AlissaStorySave().apply { save_name = "treasured_petals"; story_id = TREASURED_PETALS_STORY_ID }
) {

    override fun addScenarios() {
        super.addScenarios()

        check(storySave is AlissaStorySave)

//        scenarios.add(Scenario1())
//        scenarios.add(Scenario1dot5())
//        scenarios.add(Scenario2())
//        scenarios.add(Scenario3())
//        scenarios.add(Scenario4())
//        scenarios.add(FishGuyScenario())
//        //scenarios.add(Scenario5())
//        //scenarios.add(Scenario6())
//        //scenarios.add(Scenario7())
//        //scenarios.add(Scenario8())
//        scenarios.add(Scenario9())
//        //scenarios.add(Scenario10())
//        scenarios.add(Scenario11())
    }
}