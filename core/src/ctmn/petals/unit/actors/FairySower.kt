package ctmn.petals.unit.actors

import ctmn.petals.unit.TerrainPropsPack
import ctmn.petals.unit.UnitActor
import ctmn.petals.unit.UnitIds.DOLL_SOWER
import ctmn.petals.unit.component.*

class FairySower : UnitActor(
    UnitComponent(
        DOLL_SOWER,
        100,
        0,
        3,
        4,
        playerID = 1
    )
) {

    init {
        add(FollowerComponent())
        add(
            AttackComponent(
                5,
                15,
                1
            )
        )
        add(TerrainPropComponent(TerrainPropsPack.foot))
        add(MatchUpBonusComponent())
    }
}
