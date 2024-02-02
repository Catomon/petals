package ctmn.petals.unit.actors

import ctmn.petals.unit.TerrainBuffs
import ctmn.petals.unit.TerrainCosts
import ctmn.petals.unit.UnitActor
import ctmn.petals.unit.UnitIds
import ctmn.petals.unit.component.*

class CherieSpearman : UnitActor(
    UnitComponent(
        "cherie_spearman",
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
                25,
                35,
                1
            )
        )
        add(TerrainCostComponent(TerrainCosts.foot))
        add(TerrainBuffComponent(TerrainBuffs.foot))
        add(MatchUpBonusComponent().apply {
            bonuses[UnitIds.CAVALRY] = 15 to 15
            bonuses[UnitIds.HORSEMAN] = 15 to 15
            bonuses[UnitIds.CENTAUR_SPEAR] = 15 to 15
            bonuses[UnitIds.CENTAUR_SWORD] = 15 to 15
            bonuses[UnitIds.GOBLIN_BOAR] = 15 to 15
        })
    }
}
