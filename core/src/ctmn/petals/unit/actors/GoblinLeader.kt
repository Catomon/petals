package ctmn.petals.unit.actors

import ctmn.petals.unit.TerrainBuffs
import ctmn.petals.unit.TerrainCosts
import ctmn.petals.unit.UnitActor
import ctmn.petals.unit.component.*


class GoblinLeader : UnitActor(
    UnitComponent(
        "goblin_leader",
        100,
        15,
        4,
        6
    )
) {

    init {
        add(FollowerComponent())
        add(
            AttackComponent(
                30,
                45,
                1
            )
        )
        add(TerrainCostComponent(TerrainCosts.foot))
        add(TerrainBuffComponent(TerrainBuffs.foot))
        add(MatchUpBonusComponent())
    }
}
