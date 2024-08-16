package ctmn.petals.unit.actors

import ctmn.petals.unit.TerrainPropsPack
import ctmn.petals.unit.UnitActor
import ctmn.petals.unit.UnitIds.DOLL_AXE
import ctmn.petals.unit.UnitIds.EVIL_TREE
import ctmn.petals.unit.UnitIds.WATERPLANT
import ctmn.petals.unit.abilities.UnsummonAbility
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
