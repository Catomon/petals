package ctmn.petals.unit.actors.fairies

import ctmn.petals.unit.TerrainPropsPack
import ctmn.petals.unit.UnitActor
import ctmn.petals.unit.UnitIds
import ctmn.petals.unit.UnitIds.DOLL_ARMOR_SWORD
import ctmn.petals.unit.component.*

class FairyArmorSword : UnitActor(
    UnitComponent(
        DOLL_ARMOR_SWORD,
        100,
        30,
        4,
        6,
        playerID = 1
    )
) {

    init {
        add(FollowerComponent())
        add(
            AttackComponent(
                60,
                80,
                1
            )
        )
        add(TerrainPropComponent(TerrainPropsPack.foot))
        add(MatchUpBonusComponent().apply {
            bonuses[UnitIds.DOLL_BOW] = Pair(0, 25)
            bonuses[UnitIds.GOBLIN_BOW] = Pair(0, 25)
            bonuses[UnitIds.GOBLIN_CROSSBOW] = Pair(0, 25)
            bonuses[UnitIds.GOBLIN_CATAPULT] = Pair(0, 10)
            bonuses[UnitIds.GOBLIN_GALLEY] = Pair(0, 10)
            bonuses[UnitIds.CUCUMBER] = Pair(0, 10)
            bonuses[UnitIds.DOLL_PEAS] = Pair(0, 10)
        })
    }
}
