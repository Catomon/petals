package ctmn.petals.unit.actors.creatures

import ctmn.petals.unit.TerrainPropsPack
import ctmn.petals.unit.UnitActor
import ctmn.petals.unit.UnitIds.SLIME_TINY
import ctmn.petals.unit.component.*

class SlimeTiny : UnitActor(
    UnitComponent(
        SLIME_TINY,
        50,
        0,
        4,
        5
    )
) {

    init {
        add(FollowerComponent())
        add(
            AttackComponent(
                15,
                20,
                1
            )
        )
        add(TerrainPropComponent(TerrainPropsPack.slime))
        add(MatchUpBonusComponent())
        add(TraitComponent(fireVulnerability = 1.5f))

        hitSounds = arrayOf("slime_hit.ogg")
    }
}