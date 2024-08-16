package ctmn.petals.unit.actors

import ctmn.petals.unit.TerrainPropsPack
import ctmn.petals.unit.UnitActor
import ctmn.petals.unit.UnitIds.GOBLIN_GIANT
import ctmn.petals.unit.abilities.UnsummonAbility
import ctmn.petals.unit.component.*

class GoblinGiant : UnitActor(
    UnitComponent(
        GOBLIN_GIANT,
        300,
        30,
        3,
        6
    )
) {

    init {
        add(SummonableComponent(100))
        add(FollowerComponent())
        add(
            AttackComponent(
                55,
                70,
                1
            )
        )
        add(TerrainPropComponent(TerrainPropsPack.giant))
        add(MatchUpBonusComponent().apply {

        })
    }
}
