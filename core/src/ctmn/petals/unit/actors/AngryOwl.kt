package ctmn.petals.unit.actors

import ctmn.petals.unit.TerrainBuffs
import ctmn.petals.unit.TerrainCosts
import ctmn.petals.unit.UnitActor
import ctmn.petals.unit.UnitIds
import ctmn.petals.unit.component.*

class AngryOwl : UnitActor(
    UnitComponent(
        UnitIds.ANGRY_OWL,
        100,
        0,
        5,
        8
    )
) {

    init {
        add(FollowerComponent())
        add(
            AttackComponent(
                15,
                20,
                1
            )
        )
        add(TerrainCostComponent(TerrainCosts.flier))
        add(TerrainBuffComponent(TerrainBuffs.fly))
        add(MatchUpBonusComponent())
    }
}