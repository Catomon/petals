package ctmn.petals.unit.actors

import ctmn.petals.tile.TerrainNames
import ctmn.petals.unit.TerrainPropsPack
import ctmn.petals.unit.UnitActor
import ctmn.petals.unit.UnitIds
import ctmn.petals.unit.UnitIds.DOLL_SCOUT
import ctmn.petals.unit.component.*


class FairyScout : UnitActor(
    UnitComponent(
        DOLL_SCOUT,
        100,
        5,
        5,
        7
    )
) {

    init {
        add(FollowerComponent())
        add(
            AttackComponent(
                40,
                60,
                1,
                attackType = ATTACK_TYPE_ALL
            )
        )
        add(TerrainPropComponent(TerrainPropsPack.foot.copy().apply {
            get(TerrainNames.mountains).mv(1)
            get(TerrainNames.chasm).mv(0)
            get(TerrainNames.skyscraper).mv(2)
            get(TerrainNames.water).mv(0)
            get(TerrainNames.deepwater).mv(0)
            get(TerrainNames.lava).mv(0)
            get(TerrainNames.swamp).mv(0)
        }))
        add(MatchUpBonusComponent().apply {
            bonuses[UnitIds.CUCUMBER] = Pair(15, 0)
            bonuses[UnitIds.CATAPULT] = Pair(15, 0)
            bonuses[UnitIds.GOBLIN_CATAPULT] = Pair(15, 0)
            bonuses[UnitIds.DOLL_BOW] = Pair(15, 0)
        })
    }
}
