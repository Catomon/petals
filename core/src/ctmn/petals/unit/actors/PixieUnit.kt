package ctmn.petals.unit.actors

import ctmn.petals.player.Player
import ctmn.petals.player.Team
import ctmn.petals.unit.TerrainBuffs
import ctmn.petals.unit.TerrainCosts
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

        add(TerrainCostComponent(TerrainCosts.flier))
        add(TerrainBuffComponent(TerrainBuffs.fly))
        add(MatchUpBonusComponent())
    }
}
