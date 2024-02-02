package ctmn.petals.unit.actors

import ctmn.petals.tile.Terrain
import ctmn.petals.unit.TerrainBuffs
import ctmn.petals.unit.TerrainCosts
import ctmn.petals.unit.UnitActor
import ctmn.petals.unit.UnitIds
import ctmn.petals.unit.component.*

class GoblinBow : UnitActor(
    UnitComponent(
        "goblin_bow",
        100,
        0,
        4,
        6
    )
) {

    init {
        add(SummonableComponent(20))
        add(FollowerComponent())
        add(
            AttackComponent(
                25,
                40,
                3
            )
        )
        add(TerrainCostComponent(TerrainCosts.foot))
        add(TerrainBuffComponent(TerrainBuffs.foot.also {
            it[Terrain.hills] = 10 to 0
            it[Terrain.mountains] = 10 to 5
            it[Terrain.tower] = 10 to 10
        }))
        add(MatchUpBonusComponent().apply {
            bonuses[UnitIds.ANGRY_OWL] = 25 to 0
        })
    }
}
