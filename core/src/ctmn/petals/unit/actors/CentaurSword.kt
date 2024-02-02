package ctmn.petals.unit.actors

import ctmn.petals.unit.TerrainBuffs
import ctmn.petals.unit.TerrainCosts
import ctmn.petals.unit.UnitActor
import ctmn.petals.unit.UnitIds
import ctmn.petals.unit.component.*

class CentaurSword : UnitActor(
    UnitComponent(
        UnitIds.CENTAUR_SWORD,
        100,
        5,
        4,
        6
    )
) {

    init {
        add(FollowerComponent())
        add(
            AttackComponent(
                45,
                65,
                1
            )
        )
        add(TerrainCostComponent(TerrainCosts.foot))
        add(TerrainBuffComponent(TerrainBuffs.foot))
        add(MatchUpBonusComponent())
    }
}
