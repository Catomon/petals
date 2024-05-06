package ctmn.petals.story.faecampaign.levels

import com.badlogic.gdx.scenes.scene2d.Event
import com.badlogic.gdx.scenes.scene2d.EventListener
import ctmn.petals.bot.EasyDuelBot
import ctmn.petals.player.newBluePlayer
import ctmn.petals.player.newRedPlayer
import ctmn.petals.playscreen.*
import ctmn.petals.playscreen.events.ActionCompletedEvent
import ctmn.petals.playscreen.events.GameOverEvent
import ctmn.petals.playscreen.events.UnitDiedEvent
import ctmn.petals.playscreen.tasks.EliminateAllEnemyUnitsTask
import ctmn.petals.playscreen.tasks.KeepPlayerUnitsAlive
import ctmn.petals.playscreen.tasks.Task
import ctmn.petals.playstage.getUnitsOfPlayer
import ctmn.petals.story.Scenario
import ctmn.petals.story.gameOverSuccess
import ctmn.petals.story.playScreen
import ctmn.petals.unit.playerId
import ctmn.petals.utils.log

class Level1 : Scenario("lv_1", "level_1") {

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
            task.state != Task.State.SUCCEEDED -> 3
            playScreen.playStage.getUnitsOfPlayer(players[0]).size < initialUnits / 2 -> 2
            else -> 1
        }
    }

    override fun makeScenario(playScreen: PlayScreen) {
        super.makeScenario(playScreen)

        playScreen.botManager.add(EasyDuelBot(players[1], playScreen))

        playScreen {
            addTask(EliminateAllEnemyUnitsTask().description("Kill enemy units")).addOnCompleteTrigger {
                //gameOverSuccess()
            }

            addTask(task)
        }
    }
}