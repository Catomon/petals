package ctmn.petals.unit.actors

import ctmn.petals.tile.TerrainNames
import ctmn.petals.unit.TerrainPropsPack
import ctmn.petals.unit.UnitActor
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
        add(TerrainPropComponent(TerrainPropsPack.giant.copy().apply {

        }))
        add(MatchUpBonusComponent().apply {

        })
    }
}
