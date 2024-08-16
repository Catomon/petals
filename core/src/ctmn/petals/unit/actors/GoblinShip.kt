package ctmn.petals.unit.actors

import ctmn.petals.unit.TerrainPropsPack
import ctmn.petals.unit.UnitActor
import ctmn.petals.unit.UnitIds.DOLL_AXE
import ctmn.petals.unit.UnitIds.EVIL_TREE
import ctmn.petals.unit.UnitIds.GOBLIN_SHIP
import ctmn.petals.unit.UnitIds.WATERPLANT
import ctmn.petals.unit.abilities.UnsummonAbility
import ctmn.petals.unit.component.*

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
