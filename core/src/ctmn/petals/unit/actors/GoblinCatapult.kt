package ctmn.petals.unit.actors

import ctmn.petals.tile.TerrainNames
import ctmn.petals.unit.TerrainPropsPack
import ctmn.petals.unit.UnitActor
import ctmn.petals.unit.component.*

class GoblinCatapult : UnitActor(
    UnitComponent(
        "goblin_catapult",
        100,
        5,
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
