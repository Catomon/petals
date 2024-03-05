package ctmn.petals.unit.actors

import ctmn.petals.unit.TerrainPropsPack
import ctmn.petals.unit.UnitActor
import ctmn.petals.unit.UnitIds.NECROMANCER
import ctmn.petals.unit.abilities.FlameAbility
import ctmn.petals.unit.abilities.HealingAbility
import ctmn.petals.unit.component.*

class Necromancer : UnitActor(
    UnitComponent(
        NECROMANCER,
        100,
        10,
        5,
        8
    )
) {

    init {
        add(
            LeaderComponent(
                14000,
                3,
                15,
                50
            )
        )
        add(
            AttackComponent(
                45,
                55,
                1
            )
        )
        add(AbilitiesComponent(
            50,
            HealingAbility().apply {
                activationRange = 1
            },
            FlameAbility().apply {
                activationRange = 1
            }
        ))
        add(TerrainPropComponent(TerrainPropsPack.foot))
        add(MatchUpBonusComponent())
    }
}
