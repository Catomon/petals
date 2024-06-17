package ctmn.petals.story.faecampaign.levels

import ctmn.petals.bot.EasyDuelBot
import ctmn.petals.bot.SimpleBot
import ctmn.petals.player.Player
import ctmn.petals.player.newBluePlayer
import ctmn.petals.player.newRedPlayer
import ctmn.petals.playscreen.CaptureBases
import ctmn.petals.playscreen.GameMode
import ctmn.petals.playscreen.PlayScreen
import ctmn.petals.playscreen.selfName
import ctmn.petals.playstage.getCapturablesOf
import ctmn.petals.playstage.getUnitsOfPlayer
import ctmn.petals.story.Scenario
import ctmn.petals.story.aliceOrNull
import ctmn.petals.story.playScreen
import ctmn.petals.tile.isBase
import ctmn.petals.unit.*
import ctmn.petals.unit.actors.Alice
import ctmn.petals.unit.actors.GoblinSword
import ctmn.petals.utils.getSurroundingUnits

class Level18 : Scenario("lv_18", "level_slimeroad") {

    init {
        players.addAll(
            newBluePlayer,
            newRedPlayer,
            Player("Slimes", 4, 4)
        )

        player = players.first()

        gameEndCondition = CaptureBases().apply { ignorePlayers.add(players[2].id) }
    }

    override fun createLevel(playScreen: PlayScreen) {
        super.createLevel(playScreen)

        playScreen.gameMode = GameMode.CRYSTALS
    }

    override fun evaluateResult() {
        if (!gameEndCondition.winners.contains(player!!.id)) {
            result = 0
            return
        }

        result = when {
            playScreen.turnManager.round <= 30 -> 3
            playScreen.turnManager.round <= 35 -> 2
            else -> 1
        }
    }

    override fun makeScenario(playScreen: PlayScreen) {
        super.makeScenario(playScreen)

        val player = player!!

        playScreen.botManager.add(EasyDuelBot(players[1], playScreen))
        playScreen.botManager.add(SimpleBot(players[2], playScreen).apply {
            simpleAI.roamingIfNoAgro = true
            simpleAI.agroRange = 1
            simpleAI.permaAgro = false
            simpleAI.roamingMaxRange = 3
        })

        playScreen.fogOfWarManager.drawFog = true

        playScreen {
            if (aliceOrNull() == null) {
                //                val base = playStage.getCapturablesOf(player).filter { it.isBase }.first()
//                playStage.addActor(Alice().player(player).position(base.tiledX, base.tiledY))

                val enemyBase = playStage.getCapturablesOf(players[1]).filter { it.isBase }.first()
                playStage.addActor(GoblinSword().player(players[1]).position(enemyBase.tiledX, enemyBase.tiledY))
            }

            var leaderId = 123
            for (slime in playStage.getUnitsOfPlayer(players[2]).filter { it.selfName == UnitIds.SLIME_LING }) {
                slime.leader(leaderId)
                playStage.getSurroundingUnits(slime).forEach { it.followerOf(slime) }
                leaderId++
            }
        }
    }
}