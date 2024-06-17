package ctmn.petals.unit.actors

import ctmn.petals.unit.TerrainPropsPack
import ctmn.petals.unit.UnitActor
import ctmn.petals.unit.UnitIds
import ctmn.petals.unit.component.*

class AngryOwl : UnitActor(
    UnitComponent(
        UnitIds.ANGRY_OWL,
        100,
        0,
        5,
        8
    )
) {

    init {
        add(FollowerComponent())
        add(
            AttackComponent(
                20,
                25,
                1,
                attackType = ATTACK_TYPE_ALL
            )
        )
        add(TerrainPropComponent(TerrainPropsPack.flier))
        add(MatchUpBonusComponent())
    }
}