package ctmn.petals.unit.actors

import ctmn.petals.effects.MissileActor
import ctmn.petals.unit.TerrainPropsPack
import ctmn.petals.unit.UnitActor
import ctmn.petals.unit.UnitIds
import ctmn.petals.unit.UnitIds.GOBLIN_GALLEY
import ctmn.petals.unit.component.*
import ctmn.petals.utils.centerX
import ctmn.petals.utils.centerY

class GoblinGalley : UnitActor(
    UnitComponent(
        GOBLIN_GALLEY,
        100,
        10,
        4,
        6,
        UNIT_TYPE_WATER
    )
) {

    override val attackEffect: MissileActor get() = MissileActor().also { it.setStart(centerX, centerY + 5) }

    init {
        add(FollowerComponent())
        add(
            AttackComponent(
                45,
                60,
                4
            )
        )
        add(TerrainPropComponent(TerrainPropsPack.waterOnly))
        add(MatchUpBonusComponent().apply {
            bonuses[UnitIds.ANGRY_OWL] = 15 to 0
            bonuses[UnitIds.DOLL_BOMBER] = 15 to 0
            bonuses[UnitIds.GOBLIN_WYVERN] = 15 to 0
        })
    }
}
