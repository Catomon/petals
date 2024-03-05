package ctmn.petals.unit.actors

import ctmn.petals.Assets
import ctmn.petals.unit.*
import ctmn.petals.unit.UnitIds.ROOT_TREE
import ctmn.petals.unit.component.*

class RootTree : UnitActor(
    UnitComponent(
        ROOT_TREE,
        100,
        25,
        0,
        6
    )
) {

    init {
        add(FollowerComponent())
        add(
            AttackComponent(
                5,
                10,
                1
            )
        )
        add(TerrainPropComponent(TerrainPropsPack.foot))
        add(MatchUpBonusComponent().apply {
            bonuses[UnitIds.DOLL_PIKE] = Pair(0, 15)
            bonuses[UnitIds.DOLL_BOW] = Pair(0, 25)
        })
    }

    override fun initView(assets: Assets) {
        super.initView(assets)

        if (viewComponent is AnimationViewComponent)
            (viewComponent as AnimationViewComponent).animation.frameDuration = 1.25f
    }
}