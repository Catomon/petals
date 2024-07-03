package ctmn.petals.unit.actors

import ctmn.petals.tile.TerrainNames
import ctmn.petals.unit.TerrainPropsPack
import ctmn.petals.unit.UnitActor
import ctmn.petals.unit.UnitIds
import ctmn.petals.unit.UnitIds.GOBLIN_CROSSBOW
import ctmn.petals.unit.component.*

class GoblinCrossbow : UnitActor(
    UnitComponent(
        GOBLIN_CROSSBOW,
        100,
        5,
        4,
        6,
        playerID = 2
    )
) {

    init {
        add(SummonableComponent(20))
        add(FollowerComponent())
        add(
            AttackComponent(
                40,
                60,
                4,
                attackType = ATTACK_TYPE_ALL
            )
        )
        add(TerrainPropComponent(TerrainPropsPack.foot.copy().also {
            it[TerrainNames.hills].ad(15, 0)
            it[TerrainNames.mountains].ad(15, 5)
            it[TerrainNames.tower].ad(15,10)
        }))
        add(MatchUpBonusComponent().apply {
            bonuses[UnitIds.ANGRY_OWL] = 25 to 0
            bonuses[UnitIds.DOLL_BOMBER] = 25 to 0
            bonuses[UnitIds.GOBLIN_WYVERN] = 25 to 0
        })

        hitSounds = arrayOf("bow.ogg")
    }
}
