package ctmn.petals.unit.actors

import ctmn.petals.Assets
import ctmn.petals.Const
import ctmn.petals.unit.TerrainBuffs
import ctmn.petals.unit.TerrainCosts
import ctmn.petals.unit.UnitActor
import ctmn.petals.unit.abilities.HealthPotionAbility
import ctmn.petals.unit.component.*

class Cherie : UnitActor(
    UnitComponent(
        "cherie",
        100,
        15,
        5,
        8,
    )
) {

    init {
        add(
            LeaderComponent(
                Const.ALICE_LEADER_ID,
                2,
                5,
                0
            )
        )
        add(
            AttackComponent(
                40,
                50,
                1
            )
        )
        add(AbilitiesComponent(
            HealthPotionAbility()
        ).apply {
            mana = 25
        })
//        add(SummonerComponent().apply {
//            addUnit(UnitIds.DOLL_PIKE, 10)
//        })

        add(
            LevelComponent(
            1,
            0,
            2,
            2,
            2,
            2,
        )
        )

        add(TerrainCostComponent(TerrainCosts.foot))
        add(TerrainBuffComponent(TerrainBuffs.foot))
        add(MatchUpBonusComponent())
    }

    override fun initView(assets: Assets) {
        super.initView(assets)

        viewComponent.flipToEnemy = false
    }
}
