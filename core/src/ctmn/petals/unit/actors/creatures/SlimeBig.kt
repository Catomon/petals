package ctmn.petals.unit.actors.creatures

import ctmn.petals.unit.TerrainPropsPack
import ctmn.petals.unit.UnitActor
import ctmn.petals.unit.UnitIds
import ctmn.petals.unit.component.*

class SlimeBig : UnitActor(
    UnitComponent(
        "slime_big",
        100,
        25,
        3,
        6
    )
) {

    init {
        add(
            AttackComponent(
                40,
                55,
                1
            )
        )
        add(TerrainPropComponent(TerrainPropsPack.slime))
        add(MatchUpBonusComponent().apply {
            bonuses[UnitIds.DOLL_BOW] = Pair(0, 25)
            bonuses[UnitIds.DOLL_PIKE] = 0 to 25
        })

        hitSounds = arrayOf("slime_hit.ogg")
    }
}