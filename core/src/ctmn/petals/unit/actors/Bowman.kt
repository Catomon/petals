package ctmn.petals.unit.actors

import ctmn.petals.tile.Terrain.mountains
import ctmn.petals.unit.TerrainBuffs
import ctmn.petals.unit.TerrainCosts
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
                3
            )
        )
        add(TerrainCostComponent(TerrainCosts.foot))
        add(TerrainBuffComponent(TerrainBuffs.foot.also {
            it[mountains] = 10 to 10
        }))
        add(MatchUpBonusComponent())
    }
}
