package ctmn.petals.unit.actors.fairies

import ctmn.petals.unit.TerrainPropsPack
import ctmn.petals.unit.UnitActor
import ctmn.petals.unit.UnitIds
import ctmn.petals.unit.UnitIds.FAIRY_SWORD
import ctmn.petals.unit.component.*

/**
 * Appearance: a fairy with a short sword.
 * Role: basic line melee.
 * Notes: no building required. Matchup bonus vs slimes/blobs.
 */
class FairySword : UnitActor(
    UnitComponent(
        FAIRY_SWORD,
        100,
        5,
        4,
        6
    )
) {

    init {
        add(SummonableComponent(10))
        add(FollowerComponent())
        add(
            AttackComponent(
                25,
                35,
                1
            )
        )
        add(TerrainPropComponent(TerrainPropsPack.foot))
        add(MatchUpBonusComponent().apply {
            bonuses[UnitIds.SLIME] = Pair(25, 0)
            bonuses[UnitIds.SLIME_LING] = Pair(25, 0)
            bonuses[UnitIds.SLIME_HUGE] = Pair(25, 0)
            bonuses[UnitIds.BLOB] = Pair(25, 0)
        })
    }
}
