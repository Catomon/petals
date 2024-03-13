package ctmn.petals.unit.actors

import ctmn.petals.tile.TerrainNames
import ctmn.petals.unit.TerrainPropsPack
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
        add(TerrainPropComponent(TerrainPropsPack.foot.also {
            it[TerrainNames.hills].ad(15, 0)
            it[TerrainNames.mountains].ad(15, 5)
            it[TerrainNames.tower].ad(15,10)
        }))
        add(MatchUpBonusComponent().apply {
            bonuses["angry_owl"] = 25 to 0
        })
    }
}
