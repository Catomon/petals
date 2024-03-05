package ctmn.petals.unit.actors

import ctmn.petals.unit.TerrainPropsPack
import ctmn.petals.unit.UnitActor
import ctmn.petals.unit.UnitIds
import ctmn.petals.unit.component.*


class Horseman : UnitActor(
    UnitComponent(
        UnitIds.HORSEMAN,
        100,
        10,
        6,
        7
    )
) {

    init {
        add(ShopComponent(300))
        add(FollowerComponent())
        add(
            AttackComponent(
                50,
                65,
                1
            )
        )
        add(TerrainPropComponent(TerrainPropsPack.horse))
        add(MatchUpBonusComponent().apply {
            bonuses[UnitIds.SW0RDMAN] = Pair(30, 25)
            bonuses[UnitIds.BOWMAN] = Pair(30, 0)
            bonuses[UnitIds.CROSSBOWMAN] = Pair(30, 0)
            bonuses[UnitIds.CATAPULT] = Pair(30, 0)
        })
    }
}
