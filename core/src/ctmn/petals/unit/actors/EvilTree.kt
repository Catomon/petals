package ctmn.petals.unit.actors

import ctmn.petals.unit.TerrainBuffs
import ctmn.petals.unit.TerrainCosts
import ctmn.petals.unit.UnitActor
import ctmn.petals.unit.UnitIds
import ctmn.petals.unit.component.*

class EvilTree : UnitActor(
    UnitComponent(
        "evil_tree",
        100,
        10,
        3,
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
            bonuses[UnitIds.DOLL_PIKE] = Pair(0, 15)
            bonuses[UnitIds.DOLL_BOW] = Pair(0, 20)
        })
    }
}