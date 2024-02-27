package ctmn.petals.unit.actors

import ctmn.petals.tile.Terrain
import ctmn.petals.unit.TerrainBuffs
import ctmn.petals.unit.TerrainCosts
import ctmn.petals.unit.UnitActor
import ctmn.petals.unit.UnitIds.DOLL_AXE
import ctmn.petals.unit.UnitIds.EVIL_TREE
import ctmn.petals.unit.abilities.UnsummonAbility
import ctmn.petals.unit.component.*

class FairyHunter : UnitActor(
    UnitComponent(
        "hunter",
        250,
        20,
        3,
        6
    )
) {

    init {
        add(AbilitiesComponent(UnsummonAbility()))
        add(SummonableComponent(100))
        add(FollowerComponent())
        add(
            AttackComponent(
                45,
                60,
                1
            )
        )
        add(TerrainCostComponent((TerrainCosts.foot.clone() as HashMap<String, Int>).apply {
            put(Terrain.water, 2)
        }))
        add(TerrainBuffComponent(TerrainBuffs.foot))
        add(MatchUpBonusComponent().apply {

        })
    }
}
