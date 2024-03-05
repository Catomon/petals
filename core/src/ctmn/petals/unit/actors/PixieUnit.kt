package ctmn.petals.unit.actors

import ctmn.petals.player.Player
import ctmn.petals.player.Team
import ctmn.petals.unit.TerrainPropsPack
import ctmn.petals.unit.UnitActor
import ctmn.petals.unit.component.*

class PixieUnit : UnitActor(
    UnitComponent(
        "pixie",
        100,
        0,
        8,
        12,
        Player.BLUE,
        Team.BLUE,
    )
) {

    init {
        add(
            AttackComponent(
                25,
                35,
                1
            )
        )

        add(TerrainPropComponent(TerrainPropsPack.flier))
        add(MatchUpBonusComponent())
    }
}
