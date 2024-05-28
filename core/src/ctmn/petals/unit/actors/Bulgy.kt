package ctmn.petals.unit.actors

import ctmn.petals.unit.TerrainPropsPack
import ctmn.petals.unit.UnitActor
import ctmn.petals.unit.UnitIds.BULGY
import ctmn.petals.unit.component.*

class Bulgy : UnitActor(
    UnitComponent(
        BULGY,
        100,
        0,
        3,
        6,
        UNIT_TYPE_WATER
    )
) {

    init {
        animationProps.attackFrame = 0.8f

        add(FollowerComponent())
        add(
            AttackComponent(
                15,
                20,
                1
            )
        )
        add(TerrainPropComponent(TerrainPropsPack.waterOnly))
        add(MatchUpBonusComponent().apply {
        })
    }

    override fun loadAnimations() {
        super.loadAnimations()

        showWaterEffect = false

        attackAnimation!!.frameDuration = 0.175f
    }
}
