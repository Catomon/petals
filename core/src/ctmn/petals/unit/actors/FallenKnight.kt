package ctmn.petals.unit.actors

import ctmn.petals.unit.TerrainPropsPack
import ctmn.petals.unit.UnitActor
import ctmn.petals.unit.UnitIds
import ctmn.petals.unit.component.*

class FallenKnight : UnitActor(
    UnitComponent(
        "fallen_knight",
        100,
        10,
        6,
        7
    )
) {

    init {
        add(FollowerComponent())
        add(
            AttackComponent(
                45,
                60,
                1
            )
        )
        add(TerrainPropComponent(TerrainPropsPack.horse))
        add(MatchUpBonusComponent().apply {
            bonuses[UnitIds.SW0RDMAN] = Pair(15, 15)
            bonuses[UnitIds.BOWMAN] = Pair(15, 0)
            bonuses[UnitIds.CATAPULT] = Pair(15, 0)
            bonuses[UnitIds.DOLL_SWORD] = Pair(15, 15)
            bonuses[UnitIds.DOLL_BOW] = Pair(15, 0)
        })
    }
}
