package ctmn.petals.unit.actors.fairies

import ctmn.petals.unit.TerrainPropsPack
import ctmn.petals.unit.UnitActor
import ctmn.petals.unit.UnitIds.WATERPLANT
import ctmn.petals.unit.component.*

object FairyWaterplantStat : UnitStat(
    WATERPLANT,
    health = 100,
    minDamage = 45,
    maxDamage = 60,
)

open class UnitStat(
    val id: String,
    val health: Int = 100,
    val minDamage: Int = 0,
    val maxDamage: Int = 0,
    val attackRange: Int = 0,
    val viewRange: Int = 0
) {

    init {
        //storeSomewhere
    }
}

//private val waterplantStat = UnitStat(
//    WATERPLANT,
//    health = 100,
//    minDamage = 45,
//    maxDamage = 60,
//)

/**
 * Appearance: a waterborne plant-fairy.
 * Role: water melee (600, damage 45-60).
 * Notes: waterOnly movement; requires no building. Only placeable/buyable at a water base.
 * This file also defines the experimental UnitStat helper used by a few units.
 */
class FairyWaterplant : UnitActor(
    UnitComponent(
        FairyWaterplantStat.id,
        FairyWaterplantStat.health,
        5,
        4,
        6,
        UNIT_TYPE_WATER
    )
) {

    init {
        add(FollowerComponent())
        add(
            AttackComponent(
                FairyWaterplantStat.minDamage,
                FairyWaterplantStat.maxDamage,
                1
            )
        )
        add(TerrainPropComponent(TerrainPropsPack.waterOnly))
        add(MatchUpBonusComponent().apply {
        })
    }
}
