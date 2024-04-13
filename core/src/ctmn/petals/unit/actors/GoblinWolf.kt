package ctmn.petals.unit.actors

import ctmn.petals.tile.TerrainNames
import ctmn.petals.unit.TerrainPropsPack
import ctmn.petals.unit.UnitActor
import ctmn.petals.unit.UnitIds
import ctmn.petals.unit.component.*


class GoblinWolf : UnitActor(
    UnitComponent(
        "goblin_wolf",
        100,
        5,
        6,
        6
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
        add(TerrainPropComponent(TerrainPropsPack.horse.copy().apply {
            get(TerrainNames.forest).mv(1)
            get(TerrainNames.mountains).mv(2)
        }))
        add(MatchUpBonusComponent().apply {
            bonuses[UnitIds.SW0RDMAN] = Pair(15, 10)
            bonuses[UnitIds.BOWMAN] = Pair(15, 0)
            bonuses[UnitIds.CROSSBOWMAN] = Pair(15, 0)
            bonuses[UnitIds.CATAPULT] = Pair(15, 0)
            bonuses[UnitIds.DOLL_BOW] = Pair(15, 0)
            bonuses[UnitIds.DOLL_SWORD] = Pair(15, 10)
        })
    }
}
