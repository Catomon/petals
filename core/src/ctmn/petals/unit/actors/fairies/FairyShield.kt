package ctmn.petals.unit.actors.fairies

import ctmn.petals.unit.TerrainPropsPack
import ctmn.petals.unit.UnitActor
import ctmn.petals.unit.UnitIds
import ctmn.petals.unit.UnitIds.DOLL_SHIELD
import ctmn.petals.unit.component.*

/**
 * Appearance: a fairy carrying a large shield.
 * Role: defensive support (300, defense 20).
 * Notes: projects a small defense aura (+5) around itself. Requires Floral Aegis.
 */
class FairyShield : UnitActor(
    UnitComponent(
     DOLL_SHIELD,
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
            bonuses[UnitIds.CUCUMBER] = 0 to 15
            bonuses[UnitIds.DOLL_BOW] = 0 to 15
            bonuses[UnitIds.GOBLIN_BOW] = 0 to 15
            bonuses[UnitIds.GOBLIN_CATAPULT] = 0 to 15
        })
    }
}
