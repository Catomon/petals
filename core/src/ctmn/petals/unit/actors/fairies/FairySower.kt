package ctmn.petals.unit.actors.fairies

import ctmn.petals.unit.*
import ctmn.petals.unit.UnitIds.DOLL_SOWER
import ctmn.petals.unit.component.*

class FairySower : UnitActor(
    UnitComponent(
        DOLL_SOWER,
        100,
        0,
        3,
        4,
        playerID = 1
    )
) {

    val buildingAnimation = findAnimation(DOLL_SOWER + "_building", loop = true)

    init {
        add(FollowerComponent())
        add(
            AttackComponent(
                0,
                0,
                0
            )
        )
        add(TerrainPropComponent(TerrainPropsPack.foot))
        add(MatchUpBonusComponent())
    }

    override fun updateView() {
        super.updateView()

        if (isBuilding || isCapturing || isBaseBuilding) {
            if (cAnimationView?.animation?.let { it != buildingAnimation } == true) {
                setAnimation(buildingAnimation, duration = 9999999f)
            }
        } else {
            setAnimation(null)
        }
    }
}
