package ctmn.petals.story.faecampaign.levels

import com.badlogic.gdx.utils.Array
import ctmn.petals.bot.EasyDuelBot
import ctmn.petals.player.newBluePlayer
import ctmn.petals.player.newRedPlayer
import ctmn.petals.playscreen.*
import ctmn.petals.playscreen.listeners.TurnsCycleListener
import ctmn.petals.playscreen.tasks.EliminateAllEnemyUnitsTask
import ctmn.petals.playscreen.tasks.KeepPlayerUnitsAlive
import ctmn.petals.playscreen.tasks.Task
import ctmn.petals.playscreen.triggers.PlayerHasNoUnits
import ctmn.petals.playscreen.triggers.TurnCycleTrigger
import ctmn.petals.playscreen.triggers.UnitsDiedTrigger
import ctmn.petals.playstage.getCapturablesOf
import ctmn.petals.playstage.getUnitsOfPlayer
import ctmn.petals.story.Scenario
import ctmn.petals.story.gameOverSuccess
import ctmn.petals.story.playScreen
import ctmn.petals.unit.UnitActor

class Level3 : Scenario("lv_3", "level_capture") {

    private val task by lazy { KeepPlayerUnitsAlive(players[1]) }

    init {
        players.addAll(
            newBluePlayer.apply { credits = 500 },
            newRedPlayer.apply { credits = -99999 },
        )

        player = players.first()

        gameEndCondition = Manual()
    }

    private var initialUnits = 0
    private lateinit var enemyUnits: Array<UnitActor>

    override fun createLevel(playScreen: PlayScreen) {
        super.createLevel(playScreen)

        initialUnits = playStage.getUnitsOfPlayer(players[0]).size

        enemyUnits = playStage.getUnitsOfPlayer(players[1])
        enemyUnits.forEach { it.remove() }

        playScreen.gameMode = GameMode.CRYSTALS
    }

    override fun evaluateResult() {
        //playScreen.taskManager.getTasks().all { it.state == Task.State.SUCCEEDED }
        result = when {
            playScreen.playStage.getCapturablesOf(players[0]).size == 4 -> 3
            playScreen.playStage.getCapturablesOf(players[0]).size == 3 -> 2
            else -> 1
        }
    }

    override fun makeScenario(playScreen: PlayScreen) {
        super.makeScenario(playScreen)

        playScreen.botManager.add(EasyDuelBot(players[1], playScreen))
        playScreen.fogOfWarManager.drawFog = true

        playScreen {
            addTask(EliminateAllEnemyUnitsTask(enemyUnits).description("Kill enemy units")).addOnCompleteTrigger {
                //gameOverSuccess()
            }

            //addTask(task)

            addTurnCycleTrigger(2).onTrigger {
                queueAddUnitAction(enemyUnits.pop())
            }
            addTurnCycleTrigger(3).onTrigger {
                queueAddUnitAction(enemyUnits.pop())
            }
            addTurnCycleTrigger(4).onTrigger {
                queueAddUnitAction(enemyUnits.pop())
            }
            addTurnCycleTrigger(5).onTrigger {
                queueAddUnitAction(enemyUnits.pop())
            }
            addTurnCycleTrigger(6).onTrigger {
                enemyUnits.forEach { queueAddUnitAction(it) }
            }

            addTrigger(PlayerHasNoUnits(players[0])).onTrigger {
                gameEndCondition.lose()
            }

            addTrigger(UnitsDiedTrigger(enemyUnits)).onTrigger {
                if (playStage.getUnitsOfPlayer(players[0]).size > 0)
                    gameEndCondition.win()
                else
                    gameEndCondition.lose()
            }
        }
    }
}