package ctmn.petals.unit.actors

import ctmn.petals.unit.TerrainPropsPack
import ctmn.petals.unit.UnitActor
import ctmn.petals.unit.UnitIds.DOLL_AXE
import ctmn.petals.unit.UnitIds.EVIL_TREE
import ctmn.petals.unit.UnitIds.GOBLIN_DUELIST
import ctmn.petals.unit.abilities.UnsummonAbility
import ctmn.petals.unit.component.*

class FairyAxe : UnitActor(
    UnitComponent(
        DOLL_AXE,
        100,
        10,
        4,
        6
    )
) {

    init {
        add(ShopComponent(100))
        add(SummonableComponent(30))
        add(FollowerComponent())
        add(
            AttackComponent(
                45,
                60,
                1
            )
        )
        add(TerrainPropComponent(TerrainPropsPack.foot))
        add(MatchUpBonusComponent().apply {
            bonuses[EVIL_TREE] = 20 to 0
            bonuses[GOBLIN_DUELIST] = 0 to 15
        })
    }
}
