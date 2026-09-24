package ctmn.petals.unit.actors.goblins

import ctmn.petals.unit.TerrainPropsPack
import ctmn.petals.unit.UnitActor
import ctmn.petals.unit.component.*

/**
 * Appearance: a rank-and-file goblin.
 * Role: unused leftover base goblin.
 * Notes: not registered in the shop and has no cost; kept as a reference/placeable actor.
 */
class Goblin : UnitActor(
    UnitComponent(
        "goblin",
        100,
        0,
        4,
        6
    )
) {

    init {
        add(FollowerComponent())
        add(
            AttackComponent(
                10,
                15,
                1
            )
        )
        add(TerrainPropComponent(TerrainPropsPack.foot))
        add(MatchUpBonusComponent())
    }
}
