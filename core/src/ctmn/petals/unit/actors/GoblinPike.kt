package ctmn.petals.unit.actors

import ctmn.petals.unit.TerrainPropsPack
import ctmn.petals.unit.UnitActor
import ctmn.petals.unit.UnitIds
import ctmn.petals.unit.component.*

class GoblinPike : UnitActor(
    UnitComponent(
        "goblin_spear",
        100,
        10,
        4,
        6
    )
) {

    init {
        add(FollowerComponent())
        add(
            AttackComponent(
                20,
                30,
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
