package ctmn.petals.unit.actors

import ctmn.petals.unit.*
import ctmn.petals.unit.component.*

class PinkSlimeLing : UnitActor(
    UnitComponent(
        "pink_slime_ling",
        30,
        0,
        5,
        6
    )
) {

    init {
        add(FollowerComponent())
        add(
            AttackComponent(
                5,
                10,
                1
            )
        )
        add(TerrainPropComponent(TerrainPropsPack.slime))
        add(TerrainPropComponent(TerrainPropsPack.slime))
        add(MatchUpBonusComponent())
        add(TraitComponent(fireVulnerability = 1.5f))
    }
}