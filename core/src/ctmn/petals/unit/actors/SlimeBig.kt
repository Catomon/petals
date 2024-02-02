package ctmn.petals.unit.actors

import ctmn.petals.unit.TerrainBuffs
import ctmn.petals.unit.TerrainCosts
import ctmn.petals.unit.UnitActor
import ctmn.petals.unit.UnitIds
import ctmn.petals.unit.component.*

class SlimeBig : UnitActor(
    UnitComponent(
        "slime_big",
        100,
        25,
        3,
        6
    )
) {

    init {
        add(
            AttackComponent(
                40,
                55,
                1
            )
        )
        add(TerrainCostComponent(TerrainCosts.slime))
        add(TerrainBuffComponent(TerrainBuffs.slime))
        add(MatchUpBonusComponent().apply {
            bonuses[UnitIds.DOLL_BOW] = Pair(0, 25)
            bonuses[UnitIds.DOLL_PIKE] = 0 to 25
        })
    }
}