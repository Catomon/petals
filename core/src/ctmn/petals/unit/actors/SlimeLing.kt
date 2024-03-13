package ctmn.petals.unit.actors

import ctmn.petals.unit.*
import ctmn.petals.unit.component.*

class SlimeLing : UnitActor(
    UnitComponent(
        "slime_ling",
        100,
        0,
        4,
        6
    )
) {

    init {
        add(FollowerComponent())
        add(
            AttackComponent(
                20,
                25,
                1
            )
        )
        add(TerrainPropComponent(TerrainPropsPack.slime))
        add(MatchUpBonusComponent())
        add(TraitComponent(fireVulnerability = 1.5f))
    }
}