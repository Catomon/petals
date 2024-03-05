package ctmn.petals.unit.actors

import ctmn.petals.Assets
import ctmn.petals.Const
import ctmn.petals.player.Player
import ctmn.petals.player.Team
import ctmn.petals.unit.*
import ctmn.petals.unit.UnitIds.ALICE_ID
import ctmn.petals.unit.UnitIds.DOLL_PIKE
import ctmn.petals.unit.abilities.SummonAbility
import ctmn.petals.unit.component.*

class Alice : UnitActor(
    UnitComponent(
        ALICE_ID,
        100,
        0,
        4,
        8,
        Player.BLUE,
        Team.BLUE,
    )
) {

    init {
        abilityCastAnimation = createAnimation("alice_casting")
        talkingAnimation = createAnimation("alice_talking", 0.30f)
        airborneAnimation = createAnimation("alice_airborne", 0.30f)
        postAirborneAnimation = createAnimation("alice_post_airborne", 0.5f)

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
                25,
                35,
                1
            )
        )
        add(AbilitiesComponent(
            SummonAbility().apply {
                cost = 0
            }
//            50,
//            HealingAbility().apply {
//                activationRange = 1
//                value = 25 },
//            FlameAbility().apply {
//                activationRange = 1
//                value = 25 }
        ).apply {
            mana = 50
        })
        add(SummonerComponent().apply {
            units.add(DOLL_PIKE)
        })

        add(LevelComponent(
            1,
            0,
            2,
            2,
            2,
            2,
        ))

        add(TerrainPropComponent(TerrainPropsPack.foot))
        add(MatchUpBonusComponent())
    }

    override fun initView(assets: Assets) {
        super.initView(assets)

        viewComponent.flipToEnemy = true
    }
}
