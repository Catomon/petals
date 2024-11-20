package ctmn.petals.unit.actors.creatures

import ctmn.petals.playscreen.PlayScreen
import ctmn.petals.playscreen.playStage
import ctmn.petals.playscreen.queueAddUnitAction
import ctmn.petals.tile.isImpassable
import ctmn.petals.unit.*
import ctmn.petals.unit.Ability
import ctmn.petals.unit.component.*
import ctmn.petals.utils.getSurroundingTiles

class BigLivingTree : UnitActor(
    UnitComponent(
        "big_evil_tree",
        100,
        30,
        0,
        6
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
        add(AbilitiesComponent(SummonOwlsAbility()))
        add(TerrainPropComponent(TerrainPropsPack.foot))
        add(MatchUpBonusComponent().apply {
            bonuses[UnitIds.DOLL_PIKE] = Pair(0, 15)
            bonuses[UnitIds.DOLL_BOW] = Pair(0, 25)
        })

        mana = 30
    }

    inner class SummonOwlsAbility : Ability("summon_owls") {

        init {
            type = Type.OTHER
            target = Target.ALL

            activationRange = 0
            range = 0

            cooldown = 5

            cost = 10
        }

        override fun activate(playScreen: PlayScreen, unitCaster: UnitActor, tileX: Int, tileY: Int): Boolean {
            val freeTiles = playStage.getSurroundingTiles(tileX, tileY)
            freeTiles.removeAll { it.isImpassable() }

            if (freeTiles.isEmpty)
                return false

            freeTiles.forEach {
                val owl = AngryOwl().followerOf(this@BigLivingTree, true).position(it.tiledX, it.tiledY)
                owl.teamId = teamId
                owl.playerId = playerId
                owl.actionPoints = 0

                playScreen.queueAddUnitAction(owl)
            }

            return true
        }
    }
}