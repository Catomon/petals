package ctmn.petals.unit.actors

import ctmn.petals.tile.TerrainNames.mountains
import ctmn.petals.unit.TerrainPropsPack
import ctmn.petals.unit.UnitActor
import ctmn.petals.unit.UnitIds
import ctmn.petals.unit.component.*

class Crossbowman : UnitActor(
    UnitComponent(
        UnitIds.CROSSBOWMAN,
        100,
        10,
        4,
        6
    )
) {

    init {
        add(ShopComponent(600))
        add(FollowerComponent())
        add(
            AttackComponent(
                50,
                60,
                3,
                attackType = ATTACK_TYPE_ALL
            )
        )
        add(TerrainPropComponent(TerrainPropsPack.foot))
        add(TerrainPropComponent(TerrainPropsPack.foot.copy().also {
            it[mountains].ad(10, 10)
        }))
        add(MatchUpBonusComponent())
    }
}
