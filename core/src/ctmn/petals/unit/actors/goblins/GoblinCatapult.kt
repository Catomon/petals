package ctmn.petals.unit.actors.goblins

import ctmn.petals.effects.MissileActor
import ctmn.petals.tile.TerrainNames
import ctmn.petals.unit.ONE_TILE
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
                55,
                70,
                5,
                2,
                attackSplashDamage = 25,
                attackSplashRange = 1,
            )
        )
        add(ReloadingComponent(1))
        add(TerrainPropComponent(TerrainPropsPack.foot.copy().also {
            it[TerrainNames.mountains].ad(10, 0)
            it[TerrainNames.water].mv(ONE_TILE)
        }))
        add(MatchUpBonusComponent())
    }
}
