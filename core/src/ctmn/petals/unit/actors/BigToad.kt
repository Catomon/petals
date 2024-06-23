package ctmn.petals.unit.actors

import ctmn.petals.unit.*
import ctmn.petals.unit.component.*

class BigToad : UnitActor(
    UnitComponent(
        "big_toad",
        100,
        10,
        5,
        6
    )
) {

    init {
        add(FollowerComponent())
        add(
            AttackComponent(
                20,
                30,
                1
            )
        )
        add(TerrainPropComponent(TerrainPropsPack.slime))
        add(MatchUpBonusComponent().apply {
            bonuses[UnitIds.PIXIE] = 30 to 0
        })
        add(TraitComponent(fireVulnerability = 1.5f))
    }
}