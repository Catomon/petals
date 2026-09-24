package ctmn.petals.unit.actors.fairies

import ctmn.petals.effects.MissileActor
import ctmn.petals.tile.TerrainNames
import ctmn.petals.unit.ONE_TILE
import ctmn.petals.unit.TerrainPropsPack
import ctmn.petals.unit.UnitActor
import ctmn.petals.unit.UnitIds.FAIRY_SLINGER
import ctmn.petals.unit.component.*
import ctmn.petals.utils.centerX
import ctmn.petals.utils.centerY

/**
 * Appearance: a plant creature: a flower on four legs with two long leaf-arms that throws a
 * projectile.
 * Role: land siege artillery (long range, cannot hit adjacent tiles, splash).
 * Notes: moves slowly; Reloading. Requires Folia + the Bloomery building.
 */
class FairySlinger : UnitActor(
    UnitComponent(
        FAIRY_SLINGER,
        100,
        5,
        2,
        5
    )
) {

    override val attackEffect: MissileActor get() = MissileActor().also { it.setStart(centerX, centerY + 5) }

    companion object Stat : UnitStat(
        FAIRY_SLINGER,
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
