package ctmn.petals.unit.actors

import ctmn.petals.unit.TerrainPropsPack
import ctmn.petals.unit.UnitActor
import ctmn.petals.unit.UnitIds.GOBLIN_HEALER
import ctmn.petals.unit.abilities.HealthPotionAbility
import ctmn.petals.unit.component.*

class GoblinHealer : UnitActor(
    UnitComponent(
        GOBLIN_HEALER,
        100,
        0,
        4,
        5
    )
) {

    init {
        add(BonusFieldComponent(range = 1, healing = 15))
        add(AbilitiesComponent(20, HealthPotionAbility()))
        add(SummonableComponent(30))
        add(FollowerComponent())
        add(
            AttackComponent(
                20,
                25,
                1
            )
        )
        add(TerrainPropComponent(TerrainPropsPack.foot))
        add(MatchUpBonusComponent().apply {

        })
    }
}
