package ctmn.petals.unit.actors.goblins

import ctmn.petals.tile.components.BuildingComponent
import ctmn.petals.unit.*
import ctmn.petals.unit.UnitIds.GOBLIN_PICKAXE
import ctmn.petals.unit.component.*

/**
 * Appearance: a goblin miner with a pickaxe.
 * Role: worker.
 * Notes: the goblin builder. Builds bases and captures crystal tiles; it has no real attack.
 */
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

    val buildingAnimation by lazy { findAnimation(GOBLIN_PICKAXE + "_building", loop = true) }

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

        // A headless battle has no view, so there is no animation to switch to.
        if (!isViewInitialized) return

        if (isBuilding || isCapturing || isBaseBuilding) {
            if (cAnimationView?.animation?.let { it != buildingAnimation } == true) {
                setAnimation(buildingAnimation, duration = 9999999f)
            }
        } else {
            setAnimation(null)
        }
    }
}
