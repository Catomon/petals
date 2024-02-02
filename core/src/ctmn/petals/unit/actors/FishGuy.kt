package ctmn.petals.unit.actors

import ctmn.petals.unit.*
import ctmn.petals.unit.component.*

class FishGuy : UnitActor(
    UnitComponent(
        "fish_guy",
        100,
        32,
        3,
        6
    )
) {

    init {
        talkingAnimation = createAnimation("fish_guy_talking", 0.30f)

        add(
            AttackComponent(
                25,
                35,
                1
            )
        )
        add(AbilitiesComponent())
        add(TerrainCostComponent(TerrainCosts.slime))
        add(TerrainBuffComponent(TerrainBuffs.slime))
        add(MatchUpBonusComponent())
        add(TraitComponent(fireVulnerability = 2f))

        mana = 30
    }
}