package ctmn.petals.unit.actors

import ctmn.petals.unit.TerrainPropsPack
import ctmn.petals.unit.UnitActor
import ctmn.petals.unit.UnitIds.GOBLIN_WYVERN
import ctmn.petals.unit.component.*


class GoblinWyvern : UnitActor(
    UnitComponent(
        GOBLIN_WYVERN,
        100,
        15,
        3,
        7
    )
) {

    init {
        add(
            AttackComponent(
                65,
                80,
                1,
                attackType = ATTACK_TYPE_ALL
            )
        )

        add(TerrainPropComponent(TerrainPropsPack.flier.copy().apply {
//            get(TerrainNames.base).mv(999)
//            get(TerrainNames.crystals).mv(999)
        }))
        add(MatchUpBonusComponent())
    }

    override fun loadAnimations() {
        super.loadAnimations()

        showWaterEffect = false
    }
}
