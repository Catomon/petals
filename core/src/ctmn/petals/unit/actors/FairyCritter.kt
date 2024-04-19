package ctmn.petals.unit.actors

import ctmn.petals.tile.TerrainNames
import ctmn.petals.unit.TerrainPropsPack
import ctmn.petals.unit.UnitActor
import ctmn.petals.unit.UnitIds
import ctmn.petals.unit.component.*


class FairyCritter : UnitActor(
    UnitComponent(
        "doll_scout",
        100,
        5,
        6,
        7
    )
) {

    init {
        add(FollowerComponent())
        add(
            AttackComponent(
                40,
                60,
                1
            )
        )
        add(TerrainPropComponent(TerrainPropsPack.foot.copy().apply {
            get(TerrainNames.mountains).mv(1)
        }))
        add(MatchUpBonusComponent().apply {
            bonuses[UnitIds.CUCUMBER] = Pair(15, 15)
            bonuses[UnitIds.CATAPULT] = Pair(15, 15)
            bonuses[UnitIds.GOBLIN_CATAPULT] = Pair(15, 15)
            bonuses[UnitIds.DOLL_BOW] = Pair(15, 15)
        })
    }
}
