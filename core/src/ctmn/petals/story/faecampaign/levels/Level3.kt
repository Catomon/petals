package ctmn.petals.story.faecampaign.levels

import ctmn.petals.bot.EasyDuelBot
import ctmn.petals.player.newBluePlayer
import ctmn.petals.player.newRedPlayer
import ctmn.petals.playscreen.*
import ctmn.petals.playscreen.gui.widgets.StoryDialog
import ctmn.petals.playscreen.tasks.EliminateAllEnemyUnitsTask
import ctmn.petals.playscreen.tasks.KeepPlayerUnitsAlive
import ctmn.petals.playstage.getUnitsOfPlayer
import ctmn.petals.story.Scenario
import ctmn.petals.story.playScreen

class Level3 : Scenario("lv_3", "level_2") {

    private val task by lazy { KeepPlayerUnitsAlive(players[1]).apply { description = null } }

    init {
        players.addAll(
            newBluePlayer,
            newRedPlayer,
        )

        player = players.first()

        gameEndCondition = EliminateEnemyUnits()

        result = 3
    }

    private var initialUnits = 0

    override fun createLevel(playScreen: PlayScreen) {
        super.createLevel(playScreen)

        initialUnits = playStage.getUnitsOfPlayer(players[0]).size
    }

    override fun evaluateResult() {
        //playScreen.taskManager.getTasks().all { it.state == Task.State.SUCCEEDED }
        result = when {
            //task.state == Task.State.SUCCEEDED -> 3
            playScreen.playStage.getUnitsOfPlayer(players[0]).size >= initialUnits - 1 -> 3
            playScreen.playStage.getUnitsOfPlayer(players[0]).size >= initialUnits / 2 -> 2
            else -> 1
        }
    }

    override fun makeScenario(playScreen: PlayScreen) {
        super.makeScenario(playScreen)

        playScreen.guiStage.showCredits = false
        playScreen.botManager.add(EasyDuelBot(players[1], playScreen))

        playScreen {
            queueDialogAction(
                StoryDialog.Quote("Each unit type has its own match up bonuses"))

            queueDialogAction(
                StoryDialog.Quote("For example, pikes have bonus damage and defense against riders."))

            addTask(EliminateAllEnemyUnitsTask().description("Eliminate all enemy units")).addOnCompleteTrigger {
                //gameOverSuccess()
            }

            addTask(task)
        }
    }
}