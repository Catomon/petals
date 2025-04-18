package ctmn.petals.story.faecampaign.levels

import com.badlogic.gdx.utils.Array
import ctmn.petals.bot.EasyDuelBot
import ctmn.petals.bot.SimpleBot
import ctmn.petals.player.*
import ctmn.petals.playscreen.*
import ctmn.petals.story.Scenario
import ctmn.petals.story.playScreen
import ctmn.petals.unit.UnitActor
import ctmn.petals.unit.UnitIds

class Level7 : Scenario("lv_7", "level_swamp") {

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
            playScreen.turnManager.round <= 20 -> 3
            playScreen.turnManager.round <= 25 -> 2
            else -> 1
        }
    }

    override fun makeScenario(playScreen: PlayScreen) {
        super.makeScenario(playScreen)

        val player = player!!

        playScreen.botManager.add(EasyDuelBot(players[1], playScreen))
        playScreen.botManager.add(SimpleBot(players[2], playScreen).apply {
            simpleCommandProcessor.roamingIfNoAgro = true
            simpleCommandProcessor.agroRange = 1
            simpleCommandProcessor.permaAgro = false
            simpleCommandProcessor.roamingMaxRange = 3
        })

        playScreen.fogOfWarManager.drawFog = true
        playScreen.guiStage.buyMenu.availableUnits[player.id] = Array<UnitActor>().also {units ->
            fairyUnits.units.filter { unit ->
                unit.selfName == UnitIds.DOLL_SOWER
                        || unit.selfName == UnitIds.DOLL_AXE
                        || unit.selfName == UnitIds.DOLL_SWORD
                        || unit.selfName == UnitIds.DOLL_PIKE
                        || unit.selfName == UnitIds.DOLL_BOW
                        || unit.selfName == UnitIds.DOLL_SCOUT
                        || unit.selfName == UnitIds.DOLL_HEALER
            }.forEach {
                units.add(it)
            }

            check(!units.isEmpty)
        }

        playScreen {

        }
    }
}