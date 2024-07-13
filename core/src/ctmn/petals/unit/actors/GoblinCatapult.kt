package ctmn.petals.unit.actors

import ctmn.petals.effects.MissileActor
import ctmn.petals.tile.TerrainNames
import ctmn.petals.unit.TerrainPropsPack
import ctmn.petals.unit.UnitActor
import ctmn.petals.unit.UnitIds
import ctmn.petals.unit.UnitIds.GOBLIN_CATAPULT
import ctmn.petals.unit.component.*
import ctmn.petals.utils.centerX
import ctmn.petals.utils.centerY

class GoblinCatapult : UnitActor(
    UnitComponent(
        GOBLIN_CATAPULT,
        100,
        0,
        3,
        5
    )
) {

    override val attackEffect: MissileActor get() = MissileActor().also { it.setStart(centerX, centerY + 5) }

    init {
        actionPointsMove = 2

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
        add(ReloadingComponent(1))
        add(TerrainPropComponent(TerrainPropsPack.foot.copy().also {
            it[TerrainNames.mountains].ad(10, 0)
        }))
        add(MatchUpBonusComponent().apply {
            bonuses[UnitIds.ANGRY_OWL] = 15 to 0
            bonuses[UnitIds.DOLL_BOMBER] = 15 to 0
            bonuses[UnitIds.GOBLIN_WYVERN] = 15 to 0
        })
    }
}
