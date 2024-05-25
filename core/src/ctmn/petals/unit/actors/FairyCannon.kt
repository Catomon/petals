package ctmn.petals.unit.actors

import ctmn.petals.effects.MissileActor
import ctmn.petals.tile.TerrainNames
import ctmn.petals.unit.TerrainPropsPack
import ctmn.petals.unit.UnitActor
import ctmn.petals.unit.UnitIds
import ctmn.petals.unit.UnitIds.DOLL_CANNON
import ctmn.petals.unit.component.*
import ctmn.petals.utils.centerX
import ctmn.petals.utils.centerY

class FairyCannon : UnitActor(
    UnitComponent(
        DOLL_CANNON,
        100,
        0,
        2,
        5
    )
) {

    override val attackEffect: MissileActor get() = MissileActor().also { it.setStart(centerX, centerY + 5) }

    companion object Stat : UnitStat(
        DOLL_CANNON,
        100,
        60,
        70
    )

    init {
        animationProps.attackEffectFrame = 0f

        add(SummonableComponent(50))
        add(FollowerComponent())
        add(
            AttackComponent(
                50,
                60,
                5,
                2,
                attackSplashDamage = 15,
                attackSplashRange = 1,
            )
        )
        add(TerrainPropComponent(TerrainPropsPack.foot.copy().apply {
            get(TerrainNames.mountains).ad(10, 0)
            get(TerrainNames.water).mv(1)
        }))
        add(MatchUpBonusComponent().apply {
            bonuses[UnitIds.ANGRY_OWL] = 15 to 0
            bonuses[UnitIds.DOLL_BOMBER] = 15 to 0
            bonuses[UnitIds.GOBLIN_WYVERN] = 15 to 0
        })
    }

    override fun loadAnimations() {
        super.loadAnimations()

        attackAnimation?.frameDuration = 0.2f
    }
}
