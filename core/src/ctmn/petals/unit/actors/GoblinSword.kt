package ctmn.petals.unit.actors

import ctmn.petals.unit.TerrainPropsPack
import ctmn.petals.unit.UnitActor
import ctmn.petals.unit.UnitIds
import ctmn.petals.unit.UnitIds.GOBLIN_SWORD
import ctmn.petals.unit.component.*

class GoblinSword : UnitActor(
    UnitComponent(
        GOBLIN_SWORD,
        100,
        5,
        4,
        6
    )
) {

    init {
        add(FollowerComponent())
        add(
            AttackComponent(
                30,
                40,
                1
            )
        )
        add(TerrainPropComponent(TerrainPropsPack.foot))
        add(MatchUpBonusComponent().apply {
            bonuses[UnitIds.SLIME] = Pair(10, 0)
            bonuses[UnitIds.SLIME_LING] = Pair(10, 0)
            bonuses[UnitIds.SLIME_HUGE] = Pair(10, 0)
            bonuses[UnitIds.BLOB] = Pair(10, 0)
        })
    }
}
