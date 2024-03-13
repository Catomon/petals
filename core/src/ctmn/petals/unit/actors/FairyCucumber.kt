package ctmn.petals.unit.actors

import ctmn.petals.tile.TerrainNames
import ctmn.petals.unit.Terrain
import ctmn.petals.unit.TerrainPropsPack
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
        add(TerrainPropComponent(TerrainPropsPack.foot.copy().apply {
            get(TerrainNames.mountains).ad(10, 0)
            get(TerrainNames.water).mv(1)
        }))
        add(MatchUpBonusComponent())
    }
}
