package ctmn.petals.unit.actors.goblins

import ctmn.petals.effects.MissileActor
import ctmn.petals.tile.TerrainNames
import ctmn.petals.unit.TerrainPropsPack
import ctmn.petals.unit.UnitActor
import ctmn.petals.unit.UnitIds.GOBLIN_BOMBER
import ctmn.petals.unit.component.*
import ctmn.petals.utils.centerX
import ctmn.petals.utils.centerY

class GoblinBomber : UnitActor(
    UnitComponent(
        GOBLIN_BOMBER,
        100,
        5,
        4,
        6
    )
) {

    override val attackEffect: MissileActor
        get() = MissileActor(missileName = "bomb_missile", power = 150f).also {
            it.setStart(
                centerX,
                centerY + 5
            )
        }

    init {
        add(FollowerComponent())
        add(
            AttackComponent(
                25,
                45,
                3,
                0,
                attackSplashDamage = 35,
                attackSplashRange = 1,
                attackType = ATTACK_TYPE_GROUND,
                environmentDmg = 50
            )
        )
        add(ReloadingComponent(1))
        add(TerrainPropComponent(TerrainPropsPack.foot.copy().also {
            it[TerrainNames.hills].ad(15, 0)
            it[TerrainNames.mountains].ad(15, 5)
            it[TerrainNames.tower].ad(15, 10)
        }))
        add(MatchUpBonusComponent())
    }
}
