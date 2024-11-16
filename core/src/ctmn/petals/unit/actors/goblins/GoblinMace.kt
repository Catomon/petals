package ctmn.petals.unit.actors.goblins

import ctmn.petals.unit.TerrainPropsPack
import ctmn.petals.unit.UnitActor
import ctmn.petals.unit.UnitIds
import ctmn.petals.unit.abilities.HammerAbility
import ctmn.petals.unit.component.*

class GoblinMace : UnitActor(
    UnitComponent(
        UnitIds.GOBLIN_MACE,
        100,
        25,
        4,
        6
    )
) {

    init {
        add(FollowerComponent())
        add(
            AttackComponent(
                60,
                70,
                1,
                 environmentDmg = 30
            )
        )
        add(AbilitiesComponent(15, HammerAbility().also { it.damage = 60 }))
        add(TerrainPropComponent(TerrainPropsPack.foot))
        add(MatchUpBonusComponent().apply {
            bonuses[UnitIds.ROCK] = 20 to 0
        })
    }
}
