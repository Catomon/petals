package ctmn.petals.unit.actors

import ctmn.petals.unit.TerrainPropsPack
import ctmn.petals.unit.UnitActor
import ctmn.petals.unit.component.*

class Slime : UnitActor(
    UnitComponent(
        "slime",
        100,
        15,
        4,
        6
    )
) {

    init {
        add(
            AttackComponent(
                35,
                40,
                1
            )
        )
        add(TerrainPropComponent(TerrainPropsPack.slime))
        add(MatchUpBonusComponent())
    }
}