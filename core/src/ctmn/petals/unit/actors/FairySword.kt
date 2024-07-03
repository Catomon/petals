package ctmn.petals.unit.actors

import ctmn.petals.unit.TerrainPropsPack
import ctmn.petals.unit.UnitActor
import ctmn.petals.unit.UnitIds
import ctmn.petals.unit.UnitIds.DOLL_SWORD
import ctmn.petals.unit.UnitIds.FALLEN_SPEARMAN
import ctmn.petals.unit.abilities.UnsummonAbility
import ctmn.petals.unit.component.*

class FairySword : UnitActor(
    UnitComponent(
        DOLL_SWORD,
        100,
        5,
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
                25,
                35,
                1
            )
        )
        add(TerrainPropComponent(TerrainPropsPack.foot))
        add(MatchUpBonusComponent().apply {
            bonuses[UnitIds.SLIME] = Pair(10, 0)
            bonuses[UnitIds.SLIME_LING] = Pair(10, 0)
            bonuses[UnitIds.SLIME_HUGE] = Pair(10, 0)
            bonuses[UnitIds.BLOB] = Pair(10, 0)
        })
    }
}
