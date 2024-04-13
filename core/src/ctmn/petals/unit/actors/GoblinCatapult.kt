package ctmn.petals.unit.actors

import ctmn.petals.tile.TerrainNames
import ctmn.petals.unit.TerrainPropsPack
import ctmn.petals.unit.UnitActor
import ctmn.petals.unit.UnitIds.GOBLIN_CATAPULT
import ctmn.petals.unit.component.*

class GoblinCatapult : UnitActor(
    UnitComponent(
        GOBLIN_CATAPULT,
        100,
        0,
        3,
        5
    )
) {

    init {
        add(FollowerComponent())
        add(
            AttackComponent(
                60,
                70,
                5,
                2
            )
        )
        add(TerrainPropComponent(TerrainPropsPack.foot.also {
            it[TerrainNames.mountains].ad(10, 0)
        }))
        add(MatchUpBonusComponent())
    }
}
