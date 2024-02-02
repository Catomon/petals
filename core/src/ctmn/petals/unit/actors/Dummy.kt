package ctmn.petals.unit.actors

import ctmn.petals.unit.TerrainBuffs
import ctmn.petals.unit.TerrainCosts
import ctmn.petals.unit.UnitActor
import ctmn.petals.unit.component.*

class Dummy : UnitActor(
    UnitComponent(
        "dummy",
        100,
        0,
        3,
        6
    )
) {

    init {
        add(
            AttackComponent(
                0,
                0,
                1
            )
        )
        add(TerrainCostComponent(TerrainCosts.foot))
        add(TerrainBuffComponent(TerrainBuffs.foot))
        add(MatchUpBonusComponent())
    }
}
