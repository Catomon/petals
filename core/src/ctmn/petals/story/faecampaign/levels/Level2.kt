package ctmn.petals.story.faecampaign.levels

import com.badlogic.gdx.scenes.scene2d.Actor
import ctmn.petals.bot.EasyDuelBot
import ctmn.petals.player.newBluePlayer
import ctmn.petals.player.newRedPlayer
import ctmn.petals.playscreen.*
import ctmn.petals.playscreen.gui.widgets.CharactersPanel
import ctmn.petals.playscreen.gui.widgets.StoryDialog
import ctmn.petals.playscreen.tasks.EliminateAllEnemyUnitsTask
import ctmn.petals.playscreen.tasks.KeepPlayerUnitsAlive
import ctmn.petals.playscreen.tasks.Task
import ctmn.petals.playstage.getUnitsOfPlayer
import ctmn.petals.story.Scenario
import ctmn.petals.story.playScreen

class Level2 : Scenario("lv_2", "level_1") {

    private val task by lazy { KeepPlayerUnitsAlive(players[1]) }

    init {
        players.addAll(
            newBluePlayer,
            newRedPlayer,
        )

        player = players.first()

        gameEndCondition = EliminateEnemyUnits()
    }

    private var initialUnits = 0

    override fun createLevel(playScreen: PlayScreen) {
        super.createLevel(playScreen)

        initialUnits = playStage.getUnitsOfPlayer(players[0]).size
    }

    override fun evaluateResult() {
        //playScreen.taskManager.getTasks().all { it.state == Task.State.SUCCEEDED }
        result = when {
            task.state == Task.State.SUCCEEDED -> 3
            playScreen.playStage.getUnitsOfPlayer(players[0]).size >= initialUnits / 2 -> 2
            else -> 1
        }
    }

    override fun makeScenario(playScreen: PlayScreen) {
        super.makeScenario(playScreen)

        playScreen.guiStage.showCredits = false
        playScreen.botManager.add(EasyDuelBot(players[1], playScreen))

        playScreen {
            val fairyHelper = guiStage.charactersPanel.findActor<Actor>(CharactersPanel.CHARACTER_HELPER_FAIRY)
            queueDialogAction(StoryDialog.Quote("The numbers you see on the tiles\n are attack/defense bonuses units\n can get on these tiles.\n" +
                    "The bonuses vary depending on the unit type", fairyHelper))
            queueDialogAction(StoryDialog.Quote("You can toggle visibility of terrain bonuses in the settings menu", fairyHelper))

            queueDialogAction(StoryDialog.Quote("Use terrain bonuses to your advantage and defeat the enemy", fairyHelper))

            addTask(EliminateAllEnemyUnitsTask().description("Eliminate all enemy units")).addOnCompleteTrigger {
                //gameOverSuccess()
            }

            addTask(task)
        }
    }
}