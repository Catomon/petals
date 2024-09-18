package ctmn.petals.unit.actors.goblins

import ctmn.petals.tile.TerrainNames
import ctmn.petals.unit.TerrainPropsPack
import ctmn.petals.unit.UnitActor
import ctmn.petals.unit.UnitIds
import ctmn.petals.unit.component.*
import ctmn.petals.unit.movingRange


class GoblinWolf : UnitActor(
    UnitComponent(
        "goblin_wolf",
        100,
        10,
        5,
        6
    )
) {

    init {
        add(FollowerComponent())
        add(
            AttackComponent(
                55,
                70,
                1
            )
        )
        add(MoveAfterAttackComponent(movingRange, 2))
        add(TerrainPropComponent(TerrainPropsPack.horse.copy().apply {
            get(TerrainNames.forest).mv(1).ad(15, 5)
            get(TerrainNames.mountains).mv(2)
        }))
        add(MatchUpBonusComponent().apply {
            bonuses[UnitIds.SW0RDMAN] = Pair(30, 15)
            bonuses[UnitIds.BOWMAN] = Pair(30, 15)
            bonuses[UnitIds.CROSSBOWMAN] = Pair(30, 15)
            bonuses[UnitIds.CATAPULT] = Pair(30, 15)
            bonuses[UnitIds.DOLL_BOW] = Pair(30, 15)
            bonuses[UnitIds.DOLL_SWORD] = Pair(30, 15)
        })
    }
}
