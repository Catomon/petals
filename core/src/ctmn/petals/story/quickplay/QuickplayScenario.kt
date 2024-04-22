package ctmn.petals.story.quickplay

import ctmn.petals.ai.EasyDuelBot
import ctmn.petals.ai.SimpleBot
import ctmn.petals.map.MapGenerator
import ctmn.petals.player.Player
import ctmn.petals.player.newBluePlayer
import ctmn.petals.player.newRedPlayer
import ctmn.petals.playscreen.PlayScreen
import ctmn.petals.playscreen.TeamControlsBases
import ctmn.petals.playscreen.queueAddUnitAction
import ctmn.petals.playstage.getTiles
import ctmn.petals.story.Scenario
import ctmn.petals.story.alissa.CreateUnit
import ctmn.petals.story.playScreen
import ctmn.petals.tile.components.PlayerIdComponent
import ctmn.petals.unit.actors.SlimeHuge
import ctmn.petals.unit.player
import ctmn.petals.unit.position

class QuickplayScenario : Scenario("Fae", "") {

    val progress = 0

    var bases = 1
    var crystals = 0

    init {
        players.add(newBluePlayer)
        players.add(newRedPlayer.apply { credits = 1000 })
        players.add(Player("slime", 3, 3))
        player = players.first()

        gameEndCondition = TeamControlsBases()
    }

    override fun createLevel(playScreen: PlayScreen) {
        super.createLevel(playScreen)

        when {
            progress > 0 -> {

            }
        }

        MapGenerator().generate(playStage)

        playScreen.levelId = "randomly generated level"
        playScreen.levelCreated()
    }

    override fun makeScenario(playScreen: PlayScreen) {
        super.makeScenario(playScreen)

        playScreen.fogOfWarManager.drawFog = true

        playScreen.aiManager.add(EasyDuelBot(players[1], playScreen))
        playScreen.aiManager.add(SimpleBot(players[2], playScreen).apply {
            simpleAI.roamingIfNoAgro = true
            simpleAI.agroRange = 1
            simpleAI.permaAgro = false
            simpleAI.roamingMaxRange = 2
        })

        playScreen {
            val base = playStage.getTiles().firstOrNull { it.get(PlayerIdComponent::class.java)?.playerId == player!!.id }
            if (base != null)
                queueAddUnitAction(CreateUnit.alice.player(player!!).position(base.tiledX, base.tiledY))
            queueAddUnitAction(SlimeHuge().player(players[2]).position(playStage.tiledWidth / 2, playStage.tiledHeight / 2))
        }
    }
}