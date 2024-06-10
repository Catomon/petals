package ctmn.petals.unit.actors

import ctmn.petals.unit.TerrainPropsPack
import ctmn.petals.unit.UnitActor
import ctmn.petals.unit.UnitIds.GOBLIN_PICKAXE
import ctmn.petals.unit.component.*

class GoblinPickaxe : UnitActor(
    UnitComponent(
        GOBLIN_PICKAXE,
        100,
        0,
        3,
        4,
        playerID = 2,
        teamID = 2
    )
) {

    init {
        add(FollowerComponent())
        add(
            AttackComponent(
                0,
                10,
                1
            )
        )
        add(TerrainPropComponent(TerrainPropsPack.foot))
        add(MatchUpBonusComponent())
    }
}
