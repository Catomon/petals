package ctmn.petals.unit.actors.fairies

import ctmn.petals.tile.TerrainNames
import ctmn.petals.unit.TerrainPropsPack
import ctmn.petals.unit.UnitActor
import ctmn.petals.unit.UnitIds
import ctmn.petals.unit.UnitIds.DOLL_BOW
import ctmn.petals.unit.component.*

class FairyBow : UnitActor(
    UnitComponent(
        DOLL_BOW,
        100,
        0,
        4,
        6
    )
) {

    init {
        add(ShopComponent(100))
        add(SummonableComponent(20))
        add(FollowerComponent())
        add(
            AttackComponent(
                20,
                35,
                3,
                attackType = ATTACK_TYPE_ALL,
                environmentDmg = 15
            )
        )
        add(ReloadingComponent(1))
        add(TerrainPropComponent(TerrainPropsPack.foot.copy().also {
            it[TerrainNames.hills].ad(15, 0)
            it[TerrainNames.mountains].ad(15, 5)
            it[TerrainNames.tower].ad(15,10)
        }))
        add(MatchUpBonusComponent().apply {
            bonuses[UnitIds.ANGRY_OWL] = 25 to 0
            bonuses[UnitIds.DOLL_BOMBER] = 25 to 0
            bonuses[UnitIds.GOBLIN_WYVERN] = 25 to 0
            bonuses[UnitIds.DOLL_SCOUT] = 25 to 0
            bonuses[UnitIds.PIXIE] = 25 to 0
        })

        hitSounds = arrayOf("bow.ogg")
    }
}
