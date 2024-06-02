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
import ctmn.petals.playscreen.triggers.UnitPosRectTrigger
import ctmn.petals.playscreen.triggers.UnitsDiedTrigger
import ctmn.petals.playstage.getCapturablesOf
import ctmn.petals.playstage.getUnit
import ctmn.petals.playstage.getUnitsOfPlayer
import ctmn.petals.story.Scenario
import ctmn.petals.story.gameOverSuccess
import ctmn.petals.story.playScreen
import ctmn.petals.unit.UnitActor
import ctmn.petals.unit.UnitIds
import ctmn.petals.unit.actors.FairyAxe
import ctmn.petals.unit.tiledY

class Level4 : Scenario("lv_4", "level_healer") {

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

        enemyUnits = Array<UnitActor>().apply {
            playStage.getUnitsOfPlayer(players[1]).sortedByDescending { it.tiledY }.forEach { add(it) }
        }
        enemyUnits.forEach { it.remove() }

        playScreen.gameMode = GameMode.CRYSTALS
    }

    override fun evaluateResult() {
        //playScreen.taskManager.getTasks().all { it.state == Task.State.SUCCEEDED }
        if (gameEndCondition.result != GameEndCondition.Result.WIN) {
            result = 0
            return
        }

        val playerUnits = playScreen.playStage.getUnitsOfPlayer(players[0])
        result = when {
            playerUnits.size >= initialUnits / 2 && playerUnits.any { it.selfName == UnitIds.DOLL_HEALER } -> 3
            playerUnits.size >= initialUnits / 2 -> 2
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
                StoryDialog.Quote("You got a healer unit.\n" +
                        "Keep your units close to him to heal them up"))

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

            for (unit in enemyUnits) {
                addTrigger(
                    UnitPosRectTrigger(playStage.getUnitsOfPlayer(player), 0, unit.tiledY - 4).expandTop().expandX()
                )
                    .onTrigger {
                        playStage.addActor(unit)
                    }
            }
        }
    }
}