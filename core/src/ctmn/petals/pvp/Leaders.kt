package ctmn.petals.pvp

import ctmn.petals.GameConst
import ctmn.petals.player.Player
import ctmn.petals.player.Team
import ctmn.petals.unit.UnitIds
import ctmn.petals.unit.abilities.*
import ctmn.petals.unit.actors.Alice
import ctmn.petals.unit.component.*

val newPvPAlice get() = Alice().apply {
    add(
        UnitComponent(
            UnitIds.ALICE_ID,
            100,
            0,
            4,
            8,
            Player.BLUE,
            Team.BLUE
        )
    )
    add(
        LeaderComponent(
            GameConst.ALICE_LEADER_ID,
            2,
            10,
            5
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
            castAmounts = 4
            castAmountsLeft = castAmounts
            cooldown = 4
        },
        HealingTouchAbility(),
        MeteoriteAbility(),
        PersonalBarrierAbility(),
        LightningAbility(),
        //FortressAbility(),
//            50,
//            HealingAbility().apply {
//                activationRange = 1
//                value = 25 },
//            FlameAbility().apply {
//                activationRange = 1
//                value = 25 }
    ).apply {
        mana = 10
    })
    add(SummonerComponent().apply {
        maxUnits = 25
        units.add(UnitIds.DOLL_SWORD)
        units.add(UnitIds.DOLL_PIKE)
        units.add(UnitIds.DOLL_BOW)
        units.add(UnitIds.DOLL_AXE)
        units.add(UnitIds.CUCUMBER)
    })

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
}