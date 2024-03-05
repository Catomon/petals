package ctmn.petals.unit.actors

import ctmn.petals.unit.TerrainPropsPack
import ctmn.petals.unit.UnitActor
import ctmn.petals.unit.UnitIds
import ctmn.petals.unit.component.*


class Knight : UnitActor(
    UnitComponent(
        UnitIds.KNIGHT,
        100,
        15,
        6,
        7
    )
) {

    init {
        add(ShopComponent(600))
        add(FollowerComponent())
        add(
            AttackComponent(
                65,
                75,
                1
            )
        )
        add(TerrainPropComponent(TerrainPropsPack.horse))
        add(MatchUpBonusComponent().apply {
            bonuses[UnitIds.SW0RDMAN] = Pair(30, 30)
            bonuses[UnitIds.BOWMAN] = Pair(25, 0)
            bonuses[UnitIds.CATAPULT] = Pair(25, 0)
        })
    }
}
