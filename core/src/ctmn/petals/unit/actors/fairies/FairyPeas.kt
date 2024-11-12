package ctmn.petals.unit.actors.fairies

import ctmn.petals.effects.MissileActor
import ctmn.petals.tile.TerrainNames
import ctmn.petals.unit.ONE_TILE
import ctmn.petals.unit.TerrainPropsPack
import ctmn.petals.unit.UnitActor
import ctmn.petals.unit.UnitIds.DOLL_PEAS
import ctmn.petals.unit.component.*
import ctmn.petals.utils.centerX
import ctmn.petals.utils.centerY

class FairyPeas : UnitActor(
    UnitComponent(
        DOLL_PEAS,
        100,
        5,
        2,
        5
    )
) {

    override val attackEffect: MissileActor get() = MissileActor().also { it.setStart(centerX, centerY + 5) }

    companion object Stat : UnitStat(
        DOLL_PEAS,
        100,
        60,
        70
    )

    init {
        actionPointsMove = 2
        animationProps.attackEffectFrame = 0f

        add(SummonableComponent(50))
        add(FollowerComponent())
        add(
            AttackComponent(
                45,
                60,
                5,
                2,
                attackSplashDamage = 25,
                attackSplashRange = 1,
                environmentDmg = 100
            )
        )
        add(ReloadingComponent(1))
        add(TerrainPropComponent(TerrainPropsPack.foot.copy().apply {
            get(TerrainNames.mountains).ad(10, 0)
            get(TerrainNames.water).mv(ONE_TILE)
        }))
        add(MatchUpBonusComponent())
    }

    override fun loadAnimations() {
        super.loadAnimations()

        attackAnimation?.frameDuration = 0.2f
    }
}
