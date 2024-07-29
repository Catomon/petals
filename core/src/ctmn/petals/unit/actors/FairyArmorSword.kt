package ctmn.petals.unit.actors

import ctmn.petals.unit.TerrainPropsPack
import ctmn.petals.unit.UnitActor
import ctmn.petals.unit.UnitIds
import ctmn.petals.unit.UnitIds.DOLL_ARMOR_SWORD
import ctmn.petals.unit.component.*

class FairyArmorSword : UnitActor(
    UnitComponent(
        DOLL_ARMOR_SWORD,
        100,
        20,
        4,
        6,
        playerID = 1
    )
) {

    init {
        add(ShopComponent(400))
        add(SummonableComponent(10))
        add(FollowerComponent())
        add(
            AttackComponent(
                40,
                60,
                1
            )
        )
        add(TerrainPropComponent(TerrainPropsPack.foot))
        add(MatchUpBonusComponent().apply {
            bonuses[UnitIds.DOLL_BOW] = Pair(0, 15)
            bonuses[UnitIds.GOBLIN_BOW] = Pair(0, 15)
            bonuses[UnitIds.GOBLIN_CROSSBOW] = Pair(0, 15)
            bonuses[UnitIds.GOBLIN_CATAPULT] = Pair(0, 15)
            bonuses[UnitIds.CUCUMBER] = Pair(0, 15)
            bonuses[UnitIds.DOLL_CANNON] = Pair(0, 15)
        })
    }
}
