package ctmn.petals.unit.actors

import ctmn.petals.unit.TerrainPropsPack
import ctmn.petals.unit.UnitActor
import ctmn.petals.unit.component.*

class Swordman : UnitActor(
    UnitComponent(
        "swordman",
        100,
        10,
        4,
        6
    )
) {

    init {
        add(ShopComponent(100))
        add(FollowerComponent())
        add(
            AttackComponent(
                35,
                45,
                1
            )
        )
        add(TerrainPropComponent(TerrainPropsPack.foot))
        add(MatchUpBonusComponent())
    }
}
