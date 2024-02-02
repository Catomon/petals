package ctmn.petals.unit.actors

import ctmn.petals.unit.TerrainBuffs
import ctmn.petals.unit.TerrainCosts
import ctmn.petals.unit.UnitActor
import ctmn.petals.unit.UnitIds
import ctmn.petals.unit.component.*

class Axeman : UnitActor(
    UnitComponent(
        UnitIds.AXEMAN,
        100,
        15,
        4,
        6
    )
) {

    init {
        add(ShopComponent(600))
        add(FollowerComponent())
        add(
            AttackComponent(
                60,
                70,
                1
            )
        )
        add(TerrainCostComponent(TerrainCosts.foot))
        add(TerrainBuffComponent(TerrainBuffs.foot))
        add(MatchUpBonusComponent())
    }
}
