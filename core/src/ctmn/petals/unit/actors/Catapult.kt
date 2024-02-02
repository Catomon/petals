package ctmn.petals.unit.actors

import ctmn.petals.tile.Terrain
import ctmn.petals.unit.TerrainBuffs
import ctmn.petals.unit.TerrainCosts
import ctmn.petals.unit.UnitActor
import ctmn.petals.unit.component.*

class Catapult : UnitActor(
    UnitComponent(
        "catapult",
        100,
        10,
        3,
        5
    )
) {

    init {
        add(ShopComponent(900))
        add(FollowerComponent())
        add(
            AttackComponent(
                60,
                70,
                5,
                2
            )
        )
        add(TerrainCostComponent(TerrainCosts.foot))
        add(TerrainBuffComponent(TerrainBuffs.foot.also {
            it[Terrain.mountains] = 10 to 0
        }))
        add(MatchUpBonusComponent())
    }
}
