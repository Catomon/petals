package ctmn.petals.story.faecampaign.levels

import com.badlogic.gdx.utils.Array
import ctmn.petals.bot.EasyDuelBot
import ctmn.petals.player.SpeciesUnitNotFoundExc
import ctmn.petals.player.fairyUnits
import ctmn.petals.player.newBluePlayer
import ctmn.petals.player.newRedPlayer
import ctmn.petals.playscreen.*
import ctmn.petals.playscreen.gui.widgets.StoryDialog
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
import ctmn.petals.unit.UnitIds
import ctmn.petals.unit.actors.FairyAxe

class Level5 : Scenario("lv_5", "level_capture") {

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

        val player = player!!

        playScreen.botManager.add(EasyDuelBot(players[1], playScreen))
        playScreen.fogOfWarManager.drawFog = true
        playScreen.guiStage.buyMenu.availableUnits[player.id] = Array<UnitActor>().also {
            it.add(fairyUnits.units.find { it.selfName == UnitIds.DOLL_AXE } ?: throw SpeciesUnitNotFoundExc())
        }

        playScreen {
            queueDialogAction(
                StoryDialog.Quote("When you capture crystal tiles,\n" +
                        "they give you some amount of crystals each turn."))
            queueDialogAction(
                StoryDialog.Quote("Upon capturing a green crystal tile,\n" +
                        "you claim a base where you can get units."))

            addTrigger(PlayerHasNoUnits(players[0])).onTrigger {
                gameEndCondition.lose()
            }

            addTrigger(UnitsDiedTrigger(enemyUnits)).onTrigger {
                if (playStage.getUnitsOfPlayer(players[0]).size > 0)
                    gameEndCondition.win()
                else
                    gameEndCondition.lose()
            }

            addTask(EliminateAllEnemyUnitsTask(enemyUnits).description("Kill enemy units")).addOnCompleteTrigger {
                //gameOverSuccess()
            }

            //addTask(task)

            // don't copy this
            addTurnCycleTrigger(2).onTrigger {
                queueAddUnitAction(enemyUnits.last { it.stage == null })
            }
            addTurnCycleTrigger(3).onTrigger {
                queueAddUnitAction(enemyUnits.last { it.stage == null })
            }
            addTurnCycleTrigger(4).onTrigger {
                queueAddUnitAction(enemyUnits.last { it.stage == null })
            }
            addTurnCycleTrigger(5).onTrigger {
                queueAddUnitAction(enemyUnits.last { it.stage == null })
            }
            addTurnCycleTrigger(6).onTrigger {
                enemyUnits.filter { it.stage == null }.forEach { queueAddUnitAction(it) }
            }
        }
    }
}