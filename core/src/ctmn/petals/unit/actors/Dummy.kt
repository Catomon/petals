package ctmn.petals.unit.actors

import ctmn.petals.unit.TerrainPropsPack
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
        add(TerrainPropComponent(TerrainPropsPack.foot))
        add(MatchUpBonusComponent())
    }
}
