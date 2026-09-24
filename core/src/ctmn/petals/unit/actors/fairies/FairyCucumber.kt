package ctmn.petals.unit.actors.fairies

import ctmn.petals.effects.MissileActor
import ctmn.petals.unit.TerrainPropsPack
import ctmn.petals.unit.UnitActor
import ctmn.petals.unit.UnitIds
import ctmn.petals.unit.UnitIds.CUCUMBER
import ctmn.petals.unit.component.*
import ctmn.petals.utils.centerX
import ctmn.petals.utils.centerY

/**
 * Appearance: an underwater plant creature: a closed flower with long leaves that spits a
 * projectile. (Old name "cucumber"; rename pending.)
 * Role: water siege / spitter (cannot hit adjacent tiles).
 * Notes: waterOnly movement; requires Folia + the cucumber building.
 */
class FairyCucumber : UnitActor(
    UnitComponent(
        CUCUMBER,
        100,
        0,
        2,
        5,
        UNIT_TYPE_WATER
    )
) {

    override val attackEffect: MissileActor get() = MissileActor().also { it.setStart(centerX, centerY + 5) }

    init {
        add(SummonableComponent(50))
        add(FollowerComponent())
        add(
            AttackComponent(
                60,
                70,
                5,
                2
            )
        )
        add(TerrainPropComponent(TerrainPropsPack.waterOnly))
        add(MatchUpBonusComponent())
    }
}
