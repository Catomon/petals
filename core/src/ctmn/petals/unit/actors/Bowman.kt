package ctmn.petals.unit.actors

import ctmn.petals.tile.TerrainNames.mountains
import ctmn.petals.unit.TerrainPropsPack
import ctmn.petals.unit.UnitActor
import ctmn.petals.unit.component.*

class Bowman : UnitActor(
    UnitComponent(
        "bowman",
        100,
        5,
        4,
        6
    )
) {

    init {
        add(ShopComponent(300))
        add(FollowerComponent())
        add(
            AttackComponent(
                40,
                50,
                3,
                attackType = ATTACK_TYPE_ALL
            )
        )
        add(TerrainPropComponent(TerrainPropsPack.foot.copy().also {
            it[mountains].ad(10, 10)
        }))
        add(MatchUpBonusComponent())
    }
}
