package ctmn.petals.unit.actors

import ctmn.petals.unit.TerrainPropsPack
import ctmn.petals.unit.UnitActor
import ctmn.petals.unit.UnitIds
import ctmn.petals.unit.component.*


class GoblinDuelist : UnitActor(
    UnitComponent(
        "goblin_duelist",
        100,
        5,
        6,
        6
    )
) {

    init {
        add(FollowerComponent())
        add(
            AttackComponent(
                50,
                70,
                1
            )
        )
        add(TerrainPropComponent(TerrainPropsPack.foot))
        add(MatchUpBonusComponent().apply {
            bonuses[UnitIds.CUCUMBER] = Pair(15, 15)
            bonuses[UnitIds.CATAPULT] = Pair(15, 15)
            bonuses[UnitIds.GOBLIN_CATAPULT] = Pair(15, 15)
            bonuses[UnitIds.DOLL_BOW] = Pair(15, 15)
        })
    }
}
