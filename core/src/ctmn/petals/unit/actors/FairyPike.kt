package ctmn.petals.unit.actors

import ctmn.petals.unit.TerrainBuffs
import ctmn.petals.unit.TerrainCosts
import ctmn.petals.unit.UnitActor
import ctmn.petals.unit.UnitIds
import ctmn.petals.unit.UnitIds.DOLL_PIKE
import ctmn.petals.unit.abilities.UnsummonAbility
import ctmn.petals.unit.component.*

class FairyPike : UnitActor(
    UnitComponent(
        DOLL_PIKE,
        100,
        10,
        4,
        6
    )
) {

    init {
        add(ShopComponent(100))
        add(AbilitiesComponent(UnsummonAbility()))
        add(SummonableComponent(10))
        add(FollowerComponent())
        add(
            AttackComponent(
                20,
                30,
                1
            )
        )
        add(TerrainCostComponent(TerrainCosts.foot))
        add(TerrainBuffComponent(TerrainBuffs.foot))
        add(MatchUpBonusComponent().apply {
            bonuses[UnitIds.CAVALRY] = Pair(15, 15)
            bonuses[UnitIds.FALLEN_KNIGHT] = Pair(15, 15)
            bonuses[UnitIds.CENTAUR_SPEAR] = Pair(10, 10)
            bonuses[UnitIds.CENTAUR_SWORD] = Pair(5, 10)
            bonuses[UnitIds.GOBLIN_BOAR] = Pair(15, 15)
        })
    }
}
