package ctmn.petals.unit.actors

import ctmn.petals.unit.TerrainPropsPack
import ctmn.petals.unit.UnitActor
import ctmn.petals.unit.UnitIds
import ctmn.petals.unit.UnitIds.DOLL_GLAIVE
import ctmn.petals.unit.component.*

class FairyGlaive : UnitActor(
    UnitComponent(
        DOLL_GLAIVE,
        100,
        25,
        4,
        6,
        playerID = 1
    )
) {

    init {
        add(ShopComponent(400))
        add(SummonableComponent(10))
        add(FollowerComponent())
        add(
            AttackComponent(
                35,
                50,
                1
            )
        )
        add(TerrainPropComponent(TerrainPropsPack.foot))
        add(MatchUpBonusComponent().apply {
            bonuses[UnitIds.CAVALRY] = Pair(15, 15)
            bonuses[UnitIds.FALLEN_KNIGHT] = Pair(15, 15)
            bonuses[UnitIds.CENTAUR_SPEAR] = Pair(10, 10)
            bonuses[UnitIds.CENTAUR_SWORD] = Pair(5, 10)
            bonuses[UnitIds.GOBLIN_BOAR] = Pair(15, 15)
            bonuses[UnitIds.GOBLIN_WOLF] = Pair(15, 15)
            bonuses[UnitIds.GOBLIN_WYVERN] = Pair(0, 15)
        })
    }
}
