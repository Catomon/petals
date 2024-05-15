package ctmn.petals.unit.actors

import ctmn.petals.unit.*
import ctmn.petals.unit.component.*

class SlimeTiny : UnitActor(
    UnitComponent(
        "slime_tiny",
        50,
        0,
        4,
        5
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
        add(MatchUpBonusComponent())
        add(TraitComponent(fireVulnerability = 1.5f))
    }
}