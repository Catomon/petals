package ctmn.petals.unit.actors

import ctmn.petals.unit.TerrainPropsPack
import ctmn.petals.unit.UnitActor
import ctmn.petals.unit.UnitIds
import ctmn.petals.unit.UnitIds.GOBLIN_DUELIST
import ctmn.petals.unit.component.*


class GoblinDuelist : UnitActor(
    UnitComponent(
        GOBLIN_DUELIST,
        100,
        10,
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
            bonuses[UnitIds.CUCUMBER] = Pair(20, 0)
            bonuses[UnitIds.CATAPULT] = Pair(20, 0)
            bonuses[UnitIds.GOBLIN_CATAPULT] = Pair(20, 0)
            bonuses[UnitIds.DOLL_BOW] = Pair(20, 0)
            bonuses[UnitIds.EVIL_TREE] = 20 to 0
        })
    }
}
