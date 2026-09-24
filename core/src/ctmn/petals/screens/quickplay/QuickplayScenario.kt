package ctmn.petals.screens.quickplay

import ctmn.petals.bot.v2.BotFactory
import ctmn.petals.map.MapGenerator
import ctmn.petals.player.Player
import ctmn.petals.player.newBluePlayer
import ctmn.petals.player.newRedPlayer
import ctmn.petals.playscreen.PlayScreen
import ctmn.petals.playscreen.ControlBasesWOvertime
import ctmn.petals.playscreen.queueAddUnitAction
import ctmn.petals.playstage.getTiles
import ctmn.petals.story.Scenario
import ctmn.petals.story.alissa.CreateUnit
import ctmn.petals.story.playScreen
import ctmn.petals.tile.components.PlayerIdComponent
import ctmn.petals.unit.actors.creatures.BunnySlimeHuge
import ctmn.petals.unit.player
import ctmn.petals.unit.position

class QuickplayScenario : Scenario("Fae", "") {

    val progress = 0

    var bases = 1
    var crystals = 0

    init {
        players.add(newBluePlayer)
        players.add(newRedPlayer.apply { credits = 1000 })
        players.add(Player("Slimes", 3, 3))
        player = players.first()

        gameEndCondition = ControlBasesWOvertime().apply { ignorePlayers.add(players[2].id) }
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

        playScreen.fogOfWarManager.drawDiscoverableFog = true
        playScreen.fogOfWarManager.drawFog = true

        playScreen.botManager.add(BotFactory.create(players[1], playScreen))
        playScreen.botManager.add(BotFactory.campaign(players[2], playScreen, permaAgro = false, agroRange = 1, roamingIfNoAgro = true, roamingMaxRange = 2))

        playScreen {
            val base =
                playStage.getTiles().firstOrNull { it.get(PlayerIdComponent::class.java)?.playerId == player!!.id }
            if (base != null)
                queueAddUnitAction(CreateUnit.alice.player(player!!).position(base.tiledX, base.tiledY))
            queueAddUnitAction(
                BunnySlimeHuge().player(players[2]).position(playStage.tiledWidth / 2, playStage.tiledHeight / 2)
            )
        }
    }
}