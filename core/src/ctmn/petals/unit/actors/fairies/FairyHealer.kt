package ctmn.petals.unit.actors.fairies

import ctmn.petals.unit.TerrainPropsPack
import ctmn.petals.unit.UnitActor
import ctmn.petals.unit.UnitIds.DOLL_HEALER
import ctmn.petals.unit.abilities.HealingTouchAbility
import ctmn.petals.unit.component.*

/**
 * Appearance: a fairy healer with a staff.
 * Role: support (300).
 * Notes: HealingTouch (50+25 per level, range 2) plus a small healing field. Requires Floral Aegis.
 */
class FairyHealer : UnitActor(
    UnitComponent(
        DOLL_HEALER,
    100,
    0,
    4,
    5
)
) {

    init {
        add(BonusFieldComponent(range = 1, healing = 15))
        add(AbilitiesComponent(15, HealingTouchAbility()))
        add(SummonableComponent(30))
        add(FollowerComponent())
        add(
            AttackComponent(
                15,
                20,
                1
            )
        )
        add(TerrainPropComponent(TerrainPropsPack.foot))
        add(MatchUpBonusComponent().apply {

        })
    }
}
