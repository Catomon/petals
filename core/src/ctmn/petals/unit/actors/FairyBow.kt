package ctmn.petals.unit.actors

import ctmn.petals.tile.Terrain
import ctmn.petals.unit.TerrainBuffs
import ctmn.petals.unit.TerrainCosts
import ctmn.petals.unit.UnitActor
import ctmn.petals.unit.UnitIds.DOLL_BOW
import ctmn.petals.unit.abilities.UnsummonAbility
import ctmn.petals.unit.component.*

class FairyBow : UnitActor(
    UnitComponent(
        DOLL_BOW,
        100,
        0,
        4,
        6
    )
) {

    init {
        add(ShopComponent(100))
        add(AbilitiesComponent(UnsummonAbility()))
        add(SummonableComponent(20))
        add(FollowerComponent())
        add(
            AttackComponent(
                30,
                40,
                3
            )
        )
        add(TerrainCostComponent(TerrainCosts.foot))
        add(TerrainBuffComponent(TerrainBuffs.foot.also {
            it[Terrain.hills] = 10 to 0
            it[Terrain.mountains] = 10 to 5
            it[Terrain.tower] = 10 to 10
        }))
        add(MatchUpBonusComponent().apply {
            bonuses["angry_owl"] = 25 to 0
        })
    }
}
