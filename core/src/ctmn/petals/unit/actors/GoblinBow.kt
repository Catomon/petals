package ctmn.petals.unit.actors

import ctmn.petals.tile.TerrainNames
import ctmn.petals.unit.TerrainPropsPack
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
        add(TerrainPropComponent(TerrainPropsPack.foot))
        add(TerrainPropComponent(TerrainPropsPack.foot.also {
            it[TerrainNames.hills].ad(10, 0)
            it[TerrainNames.mountains].ad(10, 5)
            it[TerrainNames.tower].ad(10, 10)
        }))
        add(MatchUpBonusComponent().apply {
            bonuses[UnitIds.ANGRY_OWL] = 25 to 0
        })
    }
}
