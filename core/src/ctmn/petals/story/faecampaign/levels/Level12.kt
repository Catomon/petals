package ctmn.petals.story.faecampaign.levels

import ctmn.petals.bot.EasyDuelBot
import ctmn.petals.player.newBluePlayer
import ctmn.petals.player.newRedPlayer
import ctmn.petals.playscreen.CaptureBases
import ctmn.petals.playscreen.GameMode
import ctmn.petals.playscreen.PlayScreen
import ctmn.petals.story.Scenario
import ctmn.petals.story.playScreen

class Level12 : Scenario("lv_12", "level_rivers") {

    init {
        players.addAll(
            newBluePlayer,
            newRedPlayer,
        )

        player = players.first()

        gameEndCondition = CaptureBases()
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

        playScreen.fogOfWarManager.drawFog = true
//        playScreen.guiStage.buyMenu.availableUnits[player.id] = Array<UnitActor>().also {units ->
//            fairyUnits.units.filter { unit ->
//                unit.selfName == UnitIds.DOLL_AXE
//                        || unit.selfName == UnitIds.DOLL_SWORD
//                        || unit.selfName == UnitIds.DOLL_PIKE
//                        || unit.selfName == UnitIds.DOLL_BOW
//                        || unit.selfName == UnitIds.DOLL_SCOUT
//                        || unit.selfName == UnitIds.DOLL_BOMBER
//            }.forEach {
//                units.add(it)
//            }
//
//            check(!units.isEmpty)
//        }

        playScreen {

        }
    }
}