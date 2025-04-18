package ctmn.petals.unit.actors.goblins

import ctmn.petals.unit.TerrainPropsPack
import ctmn.petals.unit.UnitActor
import ctmn.petals.unit.UnitIds
import ctmn.petals.unit.UnitIds.GOBLIN_SPEAR
import ctmn.petals.unit.component.*

class GoblinPike : UnitActor(
    UnitComponent(
        GOBLIN_SPEAR,
        100,
        15,
        4,
        6
    )
) {

    init {
        add(FollowerComponent())
        add(
            AttackComponent(
                20,
                35,
                1
            )
        )
        add(TerrainPropComponent(TerrainPropsPack.foot))
        add(MatchUpBonusComponent().apply {
            bonuses[UnitIds.CAVALRY] = Pair(25, 15)
            bonuses[UnitIds.FALLEN_KNIGHT] = Pair(25, 15)
            bonuses[UnitIds.CENTAUR_SPEAR] = Pair(10, 10)
            bonuses[UnitIds.CENTAUR_SWORD] = Pair(5, 10)
            bonuses[UnitIds.GOBLIN_BOAR] = Pair(25, 15)
            bonuses[UnitIds.GOBLIN_BOAR_PIKE] = Pair(20, 15)
            bonuses[UnitIds.GOBLIN_WOLF] = Pair(25, 15)
            bonuses[UnitIds.GOBLIN_WYVERN] = Pair(0, 15)
        })
    }
}
