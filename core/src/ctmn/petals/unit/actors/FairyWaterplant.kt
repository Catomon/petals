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
) {
    init {
        //todo add to some sort of global pool
    }
}

// name: Trash
// health: 12   -> 
// damage: 12

//todo move
abstract class UnitStat(
    var id: String,
    var health: Int = 100,
    var minDamage: Int = 0,
    var maxDamage: Int = 0,
    var attackRange: Int = 0,
    var viewRange: Int = 0
) {

}

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
