package ctmn.petals.story.faecampaign

import ctmn.petals.Const
import ctmn.petals.story.Story
import ctmn.petals.story.faecampaign.levels.Level1
import ctmn.petals.story.faecampaign.levels.Level2

class FaerieStory(storySave: FaerieStorySave = FaerieStorySave()) : Story(
    "Levels",
    Const.FAERIE_CAMPAIGN_ID,
    storySave
) {

    override fun initScenarios() {
        super.initScenarios()

        check(storySave is FaerieStorySave)

        scenarios.add(Level1())
        scenarios.add(Level2())
        scenarios.add(Level2())
        scenarios.add(Level2())
        scenarios.add(Level2())

        scenarios.add(Level2())
        scenarios.add(Level2())
        scenarios.add(Level2())
        scenarios.add(Level2())
        scenarios.add(Level2())

        scenarios.add(Level2())
        scenarios.add(Level2())
        scenarios.add(Level2())
        scenarios.add(Level2())
        scenarios.add(Level2())

        scenarios.add(Level2())
        scenarios.add(Level2())
        scenarios.add(Level2())
        scenarios.add(Level2())
        scenarios.add(Level2())
    }
}