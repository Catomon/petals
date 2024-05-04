package ctmn.petals.story.levels

import ctmn.petals.Const
import ctmn.petals.story.Story

class FaerieStory(storySave: FaerieStorySave = FaerieStorySave()) : Story(
    "Levels",
    Const.FAERIE_CAMPAIGN_ID,
    storySave
) {

    override fun initScenarios() {
        super.initScenarios()

        check(storySave is FaerieStorySave)

        scenarios.add(Level1())
        scenarios.add(Level1())
        scenarios.add(Level1())
        scenarios.add(Level1())
        scenarios.add(Level1())

        scenarios.add(Level1())
        scenarios.add(Level1())
        scenarios.add(Level1())
        scenarios.add(Level1())
        scenarios.add(Level1())

        scenarios.add(Level1())
        scenarios.add(Level1())
        scenarios.add(Level1())
        scenarios.add(Level1())
        scenarios.add(Level1())

        scenarios.add(Level1())
        scenarios.add(Level1())
        scenarios.add(Level1())
        scenarios.add(Level1())
        scenarios.add(Level1())
    }
}