package ctmn.petals.unit.actors.fairies

import ctmn.petals.unit.TerrainPropsPack
import ctmn.petals.unit.UnitActor
import ctmn.petals.unit.UnitIds.WATERPLANT
import ctmn.petals.unit.component.*

/**
 * Appearance: a plant creature shaped like a lily pad with teeth.
 * Role: water melee.
 * Notes: waterOnly movement; requires no building, but is only buyable/placeable at a water
 * base.
 */
class FairyWaterplant : UnitActor(
    UnitComponent(
        WATERPLANT,
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
