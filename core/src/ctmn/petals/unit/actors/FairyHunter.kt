package ctmn.petals.unit.actors

import ctmn.petals.tile.TerrainNames
import ctmn.petals.unit.TerrainPropsPack
import ctmn.petals.unit.UnitActor
import ctmn.petals.unit.UnitIds.HUNTER
import ctmn.petals.unit.abilities.UnsummonAbility
import ctmn.petals.unit.component.*

class FairyHunter : UnitActor(
    UnitComponent(
        HUNTER,
        250,
        20,
        3,
        6
    )
) {

    init {
        add(SummonableComponent(100))
        add(FollowerComponent())
        add(
            AttackComponent(
                45,
                60,
                1
            )
        )
        add(TerrainPropComponent(TerrainPropsPack.giant.copy().apply {

        }))
        add(MatchUpBonusComponent().apply {

        })
    }

    override fun loadAnimations() {
        super.loadAnimations()

        attackAnimation?.frameDuration = 0.175f
    }
}
