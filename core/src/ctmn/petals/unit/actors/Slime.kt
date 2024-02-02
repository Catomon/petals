package ctmn.petals.unit.actors

import ctmn.petals.unit.TerrainBuffs
import ctmn.petals.unit.TerrainCosts
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
        add(TerrainCostComponent(TerrainCosts.slime))
        add(TerrainBuffComponent(TerrainBuffs.slime))
        add(MatchUpBonusComponent())
    }
}