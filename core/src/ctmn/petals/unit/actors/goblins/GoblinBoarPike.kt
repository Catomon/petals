package ctmn.petals.unit.actors.goblins

import ctmn.petals.unit.TerrainPropsPack
import ctmn.petals.unit.UnitActor
import ctmn.petals.unit.UnitIds
import ctmn.petals.unit.UnitIds.GOBLIN_BOAR_PIKE
import ctmn.petals.unit.component.*


class GoblinBoarPike : UnitActor(
    UnitComponent(
        GOBLIN_BOAR_PIKE,
        100,
        25,
        5,
        6
    )
) {

    init {
        add(FollowerComponent())
        add(
            AttackComponent(
                40,
                50,
                1
            )
        )
        add(TerrainPropComponent(TerrainPropsPack.horse))
        add(MatchUpBonusComponent().apply {
            bonuses[UnitIds.SW0RDMAN] = Pair(25, 10)
            bonuses[UnitIds.BOWMAN] = Pair(30, 0)
            bonuses[UnitIds.CROSSBOWMAN] = Pair(30, 0)
            bonuses[UnitIds.CATAPULT] = Pair(25, 0)
            bonuses[UnitIds.DOLL_BOW] = Pair(30, 0)
            bonuses[UnitIds.DOLL_SWORD] = Pair(25, 10)
            bonuses[UnitIds.DOLL_AXE] = Pair(25, 10)
            bonuses[UnitIds.GOBLIN_MACE] = Pair(25, 10)
        })
    }
}
