package ctmn.petals.unit.actors

import ctmn.petals.player.Player
import ctmn.petals.player.Team
import ctmn.petals.tile.TerrainNames
import ctmn.petals.unit.TerrainPropsPack
import ctmn.petals.unit.UnitActor
import ctmn.petals.unit.component.*

class FairyPixie : UnitActor(
    UnitComponent(
        "pixie",
        75,
        0,
        4,
        8,
        playerID = Player.BLUE,
        teamID = Team.BLUE
    )
) {

    init {
        add(
            AttackComponent(
                5,
                20,
                1
            )
        )

        add(TerrainPropComponent(TerrainPropsPack.flier.copy().apply {
            get(TerrainNames.base).mv(999)
            get(TerrainNames.crystals).mv(999)
        }))
        add(MatchUpBonusComponent())
    }
}