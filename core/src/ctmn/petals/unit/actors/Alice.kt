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
import ctmn.petals.utils.RegionAnimation

class Alice : UnitActor(
    UnitComponent(
        ALICE_ID,
        100,
        0,
        4,
        8,
        playerID = Player.BLUE,
        teamID = Team.BLUE
    )
) {

    val pickUpAni = createAnimation("alice_picking", 0.25f)
    val stickAttackAni = createAnimation("alice_stick_attack", 0.25f)
    lateinit var _attackAni: RegionAnimation

    override fun loadAnimations() {
        super.loadAnimations()

        _attackAni = attackAnimation!!
    }

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
                30,
                1
            )
        )
        add(AbilitiesComponent().apply { mana = 50 })
        add(SummonerComponent().apply { units.add(DOLL_PIKE) })

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
