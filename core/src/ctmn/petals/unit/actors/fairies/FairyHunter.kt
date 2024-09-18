package ctmn.petals.unit.actors.fairies

import ctmn.petals.unit.TerrainPropsPack
import ctmn.petals.unit.UnitActor
import ctmn.petals.unit.UnitIds.HUNTER
import ctmn.petals.unit.component.*

class FairyHunter : UnitActor(
    UnitComponent(
        HUNTER,
        300,
        30,
        3,
        6
    )
) {

    init {
        add(SummonableComponent(100))
        add(FollowerComponent())
        add(
            AttackComponent(
                55,
                70,
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
