package ctmn.petals.unit.actors.fairies

import ctmn.petals.unit.TerrainPropsPack
import ctmn.petals.unit.UnitActor
import ctmn.petals.unit.UnitIds
import ctmn.petals.unit.UnitIds.FAIRY_SHIELD
import ctmn.petals.unit.component.*

/**
 * Appearance: a fairy carrying a large shield.
 * Role: defensive support.
 * Notes: projects a small defense aura around itself. Requires Floral Aegis.
 */
class FairyShield : UnitActor(
    UnitComponent(
     FAIRY_SHIELD,
    100,
    20,
    3,
    5
)
) {

    init {
        add(BonusFieldComponent(range = 1, defense = 5))
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
            bonuses[UnitIds.WATER_BUD] = 0 to 15
            bonuses[UnitIds.FAIRY_BOW] = 0 to 15
            bonuses[UnitIds.GOBLIN_BOW] = 0 to 15
            bonuses[UnitIds.GOBLIN_CATAPULT] = 0 to 15
        })
    }
}
