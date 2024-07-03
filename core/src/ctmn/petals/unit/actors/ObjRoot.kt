package ctmn.petals.unit.actors

import ctmn.petals.unit.TerrainPropsPack
import ctmn.petals.unit.UnitActor
import ctmn.petals.unit.UnitIds
import ctmn.petals.unit.component.AttackComponent
import ctmn.petals.unit.component.MatchUpBonusComponent
import ctmn.petals.unit.component.TerrainPropComponent
import ctmn.petals.unit.component.UnitComponent

class ObjRoot : UnitActor(
    UnitComponent(
        UnitIds.ROOT,
        100,
        15,
        0,
        2
    )
) {

    init {
        add(
            AttackComponent(
                0,
                0,
                0
            )
        )
        add(TerrainPropComponent(TerrainPropsPack.foot))
        add(MatchUpBonusComponent().apply {
            bonuses[UnitIds.DOLL_PIKE] = Pair(0, 15)
            bonuses[UnitIds.DOLL_BOW] = Pair(0, 20)
            bonuses[UnitIds.GOBLIN_SPEAR] = Pair(0, 15)
            bonuses[UnitIds.GOBLIN_BOW] = Pair(0, 20)
        })
    }
}