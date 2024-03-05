package ctmn.petals.unit.actors

import ctmn.petals.unit.TerrainPropsPack
import ctmn.petals.unit.UnitActor
import ctmn.petals.unit.abilities.FlameAbility
import ctmn.petals.unit.component.*

class KnightLeader : UnitActor(
    UnitComponent(
        "knight_leader",
        100,
        10,
        5,
        8
    )
) {

    init {
        add(
            LeaderComponent(
            6123,
            3,
            15,
            15
            )
        )
        add(
            AttackComponent(
                50,
                55,
                1
            )
        )
        add(AbilitiesComponent(
            30,
            FlameAbility().apply {
                activationRange = 1
            }
        ))
        add(TerrainPropComponent(TerrainPropsPack.foot))
        add(MatchUpBonusComponent())
    }
}
