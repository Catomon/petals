package ctmn.petals.unit.actors

import ctmn.petals.unit.TerrainPropsPack
import ctmn.petals.unit.UnitActor
import ctmn.petals.unit.UnitIds
import ctmn.petals.unit.UnitIds.CUCUMBER
import ctmn.petals.unit.component.*

class FairyCucumber : UnitActor(
    UnitComponent(
        CUCUMBER,
        100,
        0,
        2,
        5,
        UNIT_TYPE_WATER
    )
) {

    init {
        add(SummonableComponent(50))
        add(FollowerComponent())
        add(
            AttackComponent(
                60,
                70,
                5,
                2
            )
        )
        add(TerrainPropComponent(TerrainPropsPack.waterOnly))
        add(MatchUpBonusComponent().apply {
            bonuses[UnitIds.ANGRY_OWL] = 15 to 0
            bonuses[UnitIds.DOLL_BOMBER] = 15 to 0
            bonuses[UnitIds.GOBLIN_WYVERN] = 15 to 0
        })
    }
}
