package ctmn.petals.unit.actors.goblins

import ctmn.petals.unit.TerrainPropsPack
import ctmn.petals.unit.UnitActor
import ctmn.petals.unit.UnitIds.GOBLIN_SHIP
import ctmn.petals.unit.component.*

/**
 * Appearance: a small goblin boat.
 * Role: water melee (600, damage 45-60).
 * Notes: waterOnly movement; requires no building, but only buyable at a water base.
 */
class GoblinShip : UnitActor(
    UnitComponent(
        GOBLIN_SHIP,
        100,
        10,
        4,
        6,
        UNIT_TYPE_WATER
    )
) {

    init {
        add(FollowerComponent())
        add(
            AttackComponent(
                45,
                60,
                1
            )
        )
        add(TerrainPropComponent(TerrainPropsPack.waterOnly))
        add(MatchUpBonusComponent().apply {
        })
    }
}
