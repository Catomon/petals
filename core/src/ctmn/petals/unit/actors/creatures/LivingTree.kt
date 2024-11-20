package ctmn.petals.unit.actors.creatures

import ctmn.petals.tile.TerrainNames
import ctmn.petals.unit.TerrainPropsPack
import ctmn.petals.unit.UnitActor
import ctmn.petals.unit.UnitIds
import ctmn.petals.unit.component.*

class LivingTree : UnitActor(
    UnitComponent(
        "evil_tree",
        100,
        10,
        3,
        6
    )
) {

    init {
        add(FollowerComponent())
        add(
            AttackComponent(
                25,
                30,
                1
            )
        )
        add(TerrainPropComponent(TerrainPropsPack.foot.copy().apply {
            get(TerrainNames.forest).mv(0).ad(15, 5)
        }))
        add(MatchUpBonusComponent().apply {
            bonuses[UnitIds.DOLL_PIKE] = Pair(0, 15)
            bonuses[UnitIds.DOLL_BOW] = Pair(0, 20)
            bonuses[UnitIds.GOBLIN_SPEAR] = Pair(0, 15)
            bonuses[UnitIds.GOBLIN_BOW] = Pair(0, 20)
        })
    }
}