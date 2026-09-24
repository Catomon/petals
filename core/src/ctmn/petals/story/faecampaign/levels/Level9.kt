package ctmn.petals.story.faecampaign.levels

import com.badlogic.gdx.utils.Array
import ctmn.petals.bot.v2.BotFactory
import ctmn.petals.player.fairyUnits
import ctmn.petals.player.newBluePlayer
import ctmn.petals.player.newRedPlayer
import ctmn.petals.playscreen.CaptureBases
import ctmn.petals.playscreen.GameMode
import ctmn.petals.playscreen.PlayScreen
import ctmn.petals.playscreen.selfName
import ctmn.petals.story.Scenario
import ctmn.petals.story.playScreen
import ctmn.petals.unit.UnitActor
import ctmn.petals.unit.UnitIds

class Level9 : Scenario("lv_9", "level_threebase") {

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
            playScreen.turnManager.round <= 20 -> 3
            playScreen.turnManager.round <= 25 -> 2
            else -> 1
        }
    }

    override fun makeScenario(playScreen: PlayScreen) {
        super.makeScenario(playScreen)

        val player = player!!

        playScreen.botManager.add(BotFactory.create(players[1], playScreen))

        playScreen.fogOfWarManager.drawFog = true
        playScreen.guiStage.buyMenu.availableUnits[player.id] = Array<UnitActor>().also { units ->
            fairyUnits.units.filter { unit ->
                unit.selfName == UnitIds.FAIRY_SOWER
                        || unit.selfName == UnitIds.FAIRY_AXE
                        || unit.selfName == UnitIds.FAIRY_SWORD
                        || unit.selfName == UnitIds.FAIRY_PIKE
                        || unit.selfName == UnitIds.FAIRY_BOW
                        || unit.selfName == UnitIds.FAIRY_SCOUT
                        || unit.selfName == UnitIds.FAIRY_BOMBER
                        || unit.selfName == UnitIds.FAIRY_HEALER
                        || unit.selfName == UnitIds.FAIRY_SHIELD
                        || unit.selfName == UnitIds.PIXIE
                        || unit.selfName == UnitIds.FAIRY_SLINGER
            }.forEach {
                units.add(it)
            }

            check(!units.isEmpty)
        }

        playScreen {

        }
    }
}