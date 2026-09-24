package ctmn.petals.unit.actors.goblins

import ctmn.petals.unit.TerrainPropsPack
import ctmn.petals.unit.UnitActor
import ctmn.petals.unit.UnitIds.GOBLIN_SHIP
import ctmn.petals.unit.component.*

/**
 * Appearance: a boat with goblins in it; it pulls up close and the goblins attack from the boat.
 * Role: water melee.
 * Notes: waterOnly movement; requires no building, but is only buyable/placeable at a water base.
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
