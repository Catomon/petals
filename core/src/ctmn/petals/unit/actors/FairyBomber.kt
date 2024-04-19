package ctmn.petals.unit.actors

import ctmn.petals.tile.TerrainNames
import ctmn.petals.unit.TerrainPropsPack
import ctmn.petals.unit.UnitActor
import ctmn.petals.unit.UnitIds.DOLL_BOMBER
import ctmn.petals.unit.component.*


class FairyBomber : UnitActor(
    UnitComponent(
        DOLL_BOMBER,
        100,
        5,
        3,
        7,
        UNIT_TYPE_AIR,
    )
) {

    init {
        add(
            AttackComponent(
                40,
                60,
                1
            )
        )

        add(TerrainPropComponent(TerrainPropsPack.flier.copy().apply {
            get(TerrainNames.base).mv(999)
            get(TerrainNames.crystals).mv(999)
        }))
        add(MatchUpBonusComponent())
    }
}
