package ctmn.petals.unit.actors.fairies

import ctmn.petals.unit.TerrainPropsPack
import ctmn.petals.unit.UnitActor
import ctmn.petals.unit.UnitIds
import ctmn.petals.unit.UnitIds.DOLL_PIKE
import ctmn.petals.unit.component.*

class FairyPike : UnitActor(
    UnitComponent(
        DOLL_PIKE,
        100,
        15,
        4,
        6
    )
) {

    init {
        add(ShopComponent(100))
        add(SummonableComponent(10))
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
            bonuses[UnitIds.CAVALRY] = Pair(20, 15)
            bonuses[UnitIds.FALLEN_KNIGHT] = Pair(20, 15)
            bonuses[UnitIds.CENTAUR_SPEAR] = Pair(10, 10)
            bonuses[UnitIds.CENTAUR_SWORD] = Pair(10, 10)
            bonuses[UnitIds.GOBLIN_BOAR] = Pair(20, 15)
            bonuses[UnitIds.GOBLIN_WOLF] = Pair(20, 15)
            bonuses[UnitIds.GOBLIN_WYVERN] = Pair(0, 15)
        })
    }
}
