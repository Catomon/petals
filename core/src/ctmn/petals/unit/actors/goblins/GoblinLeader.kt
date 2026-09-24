package ctmn.petals.unit.actors.goblins

import ctmn.petals.unit.TerrainPropsPack
import ctmn.petals.unit.UnitActor
import ctmn.petals.unit.component.*


/**
 * Appearance: a goblin commander.
 * Role: unused leftover.
 * Notes: not registered in the shop and has no LeaderComponent despite the name.
 */
class GoblinLeader : UnitActor(
    UnitComponent(
        "goblin_leader",
        100,
        15,
        4,
        6
    )
) {

    init {
        add(FollowerComponent())
        add(
            AttackComponent(
                30,
                45,
                1
            )
        )
        add(TerrainPropComponent(TerrainPropsPack.foot))
        add(MatchUpBonusComponent())
    }
}
