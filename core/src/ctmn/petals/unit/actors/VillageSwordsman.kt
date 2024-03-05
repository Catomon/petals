package ctmn.petals.unit.actors

import ctmn.petals.unit.TerrainPropsPack
import ctmn.petals.unit.UnitActor
import ctmn.petals.unit.UnitIds
import ctmn.petals.unit.component.*

class VillageSwordsman : UnitActor(
    UnitComponent(
        "village_swordsman",
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
                30,
                40,
                1
            )
        )
        add(TerrainPropComponent(TerrainPropsPack.foot))
        add(MatchUpBonusComponent().apply {
            bonuses[UnitIds.SLIME] = Pair(20, 0)
            bonuses["slime_ling"] = Pair(20, 0)
            bonuses["slime_big"] = Pair(15, 0)
            bonuses["slime_huge"] = Pair(5, 0)
        })
    }
}
