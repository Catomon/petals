package ctmn.petals.unit.actors.fairies

import ctmn.petals.effects.MissileActor
import ctmn.petals.unit.TerrainPropsPack
import ctmn.petals.unit.UnitActor
import ctmn.petals.unit.UnitIds.DOLL_BOMBER
import ctmn.petals.unit.component.*
import ctmn.petals.unit.movingRange
import ctmn.petals.utils.centerX
import ctmn.petals.utils.centerY


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

    override val attackEffect: MissileActor
        get() = MissileActor(
            "cucumber_missile",
            "cucumber_explode",
            power = 75f
        ).also { it.setStart(centerX, centerY) }

    init {
        animationProps.attackEffectFrame = 0.5f

        add(
            AttackComponent(
                35,
                50,
                1,
                attackSplashDamage = 30,
                attackSplashRange = 1,
                attackType = ATTACK_TYPE_GROUND,
                environmentDmg = 50
            )
        )
        add(MoveAfterAttackComponent(movingRange, 1))
        add(ReloadingComponent(1))
        add(TerrainPropComponent(TerrainPropsPack.flier.copy().apply {
//            get(TerrainNames.base).mv(999)
//            get(TerrainNames.crystals).mv(999)
        }))
        add(MatchUpBonusComponent())
    }

    override fun loadAnimations() {
        super.loadAnimations()

        attackAnimation?.frameDuration = 0.4f
        //animationProps.attackFrame = 10f / 12f
    }
}
