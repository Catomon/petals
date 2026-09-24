package ctmn.petals.unit.actors.goblins

import ctmn.petals.unit.TerrainPropsPack
import ctmn.petals.unit.UnitActor
import ctmn.petals.unit.UnitIds.GOBLIN_SCOUT
import ctmn.petals.unit.component.*


/**
 * Appearance: a goblin floating on a small hot-air balloon.
 * Role: cheap flying scout.
 * Notes: flier movement but a LAND type, so ground units can still hit it once it lands. No
 * Follower. This is the goblin cheap scout.
 */
class GoblinScout : UnitActor(
    UnitComponent(
        GOBLIN_SCOUT,
        75,
        0,
        4,
        8,
    )
) {

    init {
        add(
            AttackComponent(
                5,
                20,
                1,
                attackType = ATTACK_TYPE_ALL
            )
        )

        add(TerrainPropComponent(TerrainPropsPack.flier.copy().apply {
//            get(TerrainNames.base).mv(999)
//            get(TerrainNames.crystals).mv(999)
        }))
        add(MatchUpBonusComponent())
    }
}
