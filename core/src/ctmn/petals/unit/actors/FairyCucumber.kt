package ctmn.petals.unit.actors

import ctmn.petals.tile.Terrain
import ctmn.petals.unit.TerrainBuffs
import ctmn.petals.unit.TerrainCosts
import ctmn.petals.unit.UnitActor
import ctmn.petals.unit.UnitIds.CUCUMBER
import ctmn.petals.unit.component.*

class FairyCucumber : UnitActor(
    UnitComponent(
        CUCUMBER,
        100,
        0,
        2,
        5
    )
) {

    init {
        add(SummonableComponent(50))
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
