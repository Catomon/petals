package ctmn.petals.unit.actors.goblins

import ctmn.petals.tile.components.BuildingComponent
import ctmn.petals.unit.*
import ctmn.petals.unit.UnitIds.GOBLIN_PICKAXE
import ctmn.petals.unit.component.*

class GoblinPickaxe : UnitActor(
    UnitComponent(
        GOBLIN_PICKAXE,
        100,
        0,
        3,
        4,
        playerID = 2,
        teamID = 2
    )
) {

    val buildingAnimation = findAnimation(GOBLIN_PICKAXE + "_building", loop = true)

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
