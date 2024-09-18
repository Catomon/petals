package ctmn.petals.unit.actors.fairies

import ctmn.petals.player.Player
import ctmn.petals.player.Team
import ctmn.petals.unit.TerrainPropsPack
import ctmn.petals.unit.UnitActor
import ctmn.petals.unit.UnitIds.PIXIE
import ctmn.petals.unit.component.*

class FairyPixie : UnitActor(
    UnitComponent(
        PIXIE,
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
                10,
                20,
                1,
                attackType = ATTACK_TYPE_ALL
            )
        )

        add(TerrainPropComponent(TerrainPropsPack.flier.copy().apply {
//            get(TerrainNames.base).mv(999)
//            get(TerrainNames.crystals).mv(999)
        }))
        add(MatchUpBonusComponent())
    }

    override fun loadAnimations() {
        super.loadAnimations()

        showWaterEffect = false
    }
}
