package ctmn.petals.unit.actors

import ctmn.petals.tile.Terrain.mountains
import ctmn.petals.unit.TerrainBuffs
import ctmn.petals.unit.TerrainCosts
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
