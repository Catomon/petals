package ctmn.petals.story.faecampaign.levels

import com.badlogic.gdx.utils.Array
import ctmn.petals.bot.EasyDuelBot
import ctmn.petals.bot.SimpleBot
import ctmn.petals.player.*
import ctmn.petals.playscreen.*
import ctmn.petals.playscreen.gui.widgets.StoryDialog
import ctmn.petals.playscreen.listeners.TurnsCycleListener
import ctmn.petals.playscreen.tasks.EliminateAllEnemyUnitsTask
import ctmn.petals.playscreen.tasks.KeepPlayerUnitsAlive
import ctmn.petals.playscreen.tasks.Task
import ctmn.petals.playscreen.triggers.PlayerHasNoUnits
import ctmn.petals.playscreen.triggers.TurnCycleTrigger
import ctmn.petals.playscreen.triggers.UnitPosRectTrigger
import ctmn.petals.playscreen.triggers.UnitsDiedTrigger
import ctmn.petals.playstage.getCapturablesOf
import ctmn.petals.playstage.getUnit
import ctmn.petals.playstage.getUnitsOfPlayer
import ctmn.petals.story.Scenario
import ctmn.petals.story.alice
import ctmn.petals.story.gameOverSuccess
import ctmn.petals.story.playScreen
import ctmn.petals.unit.UnitActor
import ctmn.petals.unit.UnitIds
import ctmn.petals.unit.actors.FairyAxe
import ctmn.petals.unit.tiledY

class Level7 : Scenario("lv_7", "alice_slime") {

    init {
        players.addAll(
            newBluePlayer,
            newRedPlayer,
            Player("Slimes", 4, 4)
        )

        player = players.first()

        gameEndCondition = Manual().apply { ignorePlayers.add(players[2].id) }
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
            playScreen.turnManager.round <= 10 -> 3
            playScreen.turnManager.round <= 15 -> 2
            else -> 1
        }
    }

    override fun makeScenario(playScreen: PlayScreen) {
        super.makeScenario(playScreen)

        val player = player!!

        playScreen.botManager.add(EasyDuelBot(players[1], playScreen))
        playScreen.botManager.add(SimpleBot(players[2], playScreen).apply {
            simpleAI.roamingIfNoAgro = true
            simpleAI.agroRange = 3
            simpleAI.permaAgro = false
            simpleAI.roamingMaxRange = 3
        })
        playScreen.fogOfWarManager.drawFog = false

        //        gameEndCondition.win()
        //                else
        //                    gameEndCondition.lose()

        playScreen {
            queueDialogAction(alice(),
                StoryDialog.Quote("Hello!"),
            )
        }
    }
}