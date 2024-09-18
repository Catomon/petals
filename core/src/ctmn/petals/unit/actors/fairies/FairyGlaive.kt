package ctmn.petals.unit.actors.fairies

import ctmn.petals.unit.TerrainPropsPack
import ctmn.petals.unit.UnitActor
import ctmn.petals.unit.UnitIds
import ctmn.petals.unit.UnitIds.DOLL_GLAIVE
import ctmn.petals.unit.component.*

class FairyGlaive : UnitActor(
    UnitComponent(
        DOLL_GLAIVE,
        100,
        35,
        4,
        6,
        playerID = 1
    )
) {

    init {
        add(FollowerComponent())
        add(
            AttackComponent(
                50,
                70,
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
            bonuses[UnitIds.DOLL_CANNON] = Pair(0, 10)

            bonuses[UnitIds.CAVALRY] = Pair(25, 15)
            bonuses[UnitIds.FALLEN_KNIGHT] = Pair(25, 15)
            bonuses[UnitIds.CENTAUR_SPEAR] = Pair(25, 15)
            bonuses[UnitIds.CENTAUR_SWORD] = Pair(25, 15)
            bonuses[UnitIds.GOBLIN_BOAR] = Pair(25, 15)
            bonuses[UnitIds.GOBLIN_WOLF] = Pair(25, 15)
            bonuses[UnitIds.GOBLIN_WYVERN] = Pair(0, 15)
        })
    }
}
