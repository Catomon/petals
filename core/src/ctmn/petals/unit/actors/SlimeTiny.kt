package ctmn.petals.unit.actors

import ctmn.petals.unit.*
import ctmn.petals.unit.component.*

class SlimeTiny : UnitActor(
    UnitComponent(
        "slime_tiny",
        75,
        0,
        4,
        5
    )
) {

    init {
        add(FollowerComponent())
        add(
            AttackComponent(
                0,
                5,
                1
            )
        )
        add(TerrainPropComponent(TerrainPropsPack.slime))
        add(MatchUpBonusComponent())
        add(TraitComponent(fireVulnerability = 1.5f))
    }
}