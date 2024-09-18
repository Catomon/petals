package ctmn.petals.unit.actors.goblins

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
                25,
                40,
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
