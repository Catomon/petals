package ctmn.petals.unit.actors

import ctmn.petals.unit.TerrainPropsPack
import ctmn.petals.unit.UnitActor
import ctmn.petals.unit.UnitIds
import ctmn.petals.unit.component.*

class CentaurSpear : UnitActor(
    UnitComponent(
        UnitIds.CENTAUR_SPEAR,
        100,
        10,
        4,
        6
    )
) {

    init {
        add(FollowerComponent())
        add(
            AttackComponent(
                50,
                60,
                1
            )
        )
        add(TerrainPropComponent(TerrainPropsPack.foot))
        add(MatchUpBonusComponent().apply {
            bonuses[UnitIds.CAVALRY] = 30 to 30
            bonuses[UnitIds.HORSEMAN] = 30 to 30
            bonuses[UnitIds.CENTAUR_SPEAR] = 15 to 15
            bonuses[UnitIds.CENTAUR_SWORD] = 30 to 20
        })
    }
}
