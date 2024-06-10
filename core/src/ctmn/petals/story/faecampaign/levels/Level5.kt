package ctmn.petals.story.faecampaign.levels

import com.badlogic.gdx.utils.Array
import ctmn.petals.bot.EasyDuelBot
import ctmn.petals.player.SpeciesUnitNotFoundExc
import ctmn.petals.player.fairyUnits
import ctmn.petals.player.newBluePlayer
import ctmn.petals.player.newRedPlayer
import ctmn.petals.playscreen.*
import ctmn.petals.playscreen.commands.BuildBaseCommand
import ctmn.petals.playscreen.gui.widgets.StoryDialog
import ctmn.petals.playscreen.tasks.EliminateAllEnemyUnitsTask
import ctmn.petals.playscreen.tasks.ExecuteCommandTask
import ctmn.petals.playscreen.tasks.KeepPlayerUnitsAlive
import ctmn.petals.playscreen.tasks.MoveUnitTask
import ctmn.petals.playscreen.triggers.PlayerHasNoUnits
import ctmn.petals.playscreen.triggers.TurnStartTrigger
import ctmn.petals.playscreen.triggers.UnitsDiedTrigger
import ctmn.petals.playstage.getCapturablesOf
import ctmn.petals.playstage.getUnit
import ctmn.petals.playstage.getUnitsOfPlayer
import ctmn.petals.story.Scenario
import ctmn.petals.story.playScreen
import ctmn.petals.unit.UnitActor
import ctmn.petals.unit.UnitIds
import ctmn.petals.unit.actors.FairySower
import ctmn.petals.unit.tiledX
import ctmn.petals.unit.tiledY

class Level5 : Scenario("lv_5", "level_capture") {

    private val task by lazy { KeepPlayerUnitsAlive(players[1]) }

    init {
        players.addAll(
            newBluePlayer.apply { credits = 800 },
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
            playScreen.playStage.getCapturablesOf(players[0]).size >= 4 -> 3
            playScreen.playStage.getCapturablesOf(players[0]).size >= 3 -> 2
            else -> 1
        }
    }

    override fun makeScenario(playScreen: PlayScreen) {
        super.makeScenario(playScreen)

        val player = player!!

        playScreen.botManager.add(EasyDuelBot(players[1], playScreen))
        playScreen.fogOfWarManager.drawFog = true
        playScreen.guiStage.buyMenu.availableUnits[player.id] = Array<UnitActor>().also {
            it.add(fairyUnits.units.find { it.selfName == UnitIds.DOLL_SOWER } ?: throw SpeciesUnitNotFoundExc())
            it.add(fairyUnits.units.find { it.selfName == UnitIds.DOLL_AXE } ?: throw SpeciesUnitNotFoundExc())
        }

        playScreen {
            queueDialogAction(
                StoryDialog.Quote(
                    "To get units, you need to set a base using Sower Fairy"
                ),
                StoryDialog.Quote(
                    "Move Sower Fairy to marked position\n" +
                            "and set a base"
                )
            )

            val fairySower = playStage.getUnit<FairySower>()!!
            queueTask(
                MoveUnitTask(
                    fairySower,
                    fairySower.tiledX + 2,
                    fairySower.tiledY + 1,
                    true
                ).description("Move Fairy Sower")
            ).addOnCompleteTrigger {
                queueTask(ExecuteCommandTask(BuildBaseCommand::class, true).description("Set a base"))
                addTrigger(TurnStartTrigger(players[0])).onTrigger {
                    queueDialogAction(
                        StoryDialog.Quote(
                            "Buy Sower Fairies to set bases and capture crystal tiles"
                        ),
                        StoryDialog.Quote(
                            "When you capture crystal tiles,\n" +
                                    "they give you some amount of crystals each turn."
                        ),
                        StoryDialog.Quote(
                            "Use them to buy more units"
                        )
                    )
                }.onTrigger {
                    addTask(EliminateAllEnemyUnitsTask(enemyUnits).description("Kill enemy units")).addOnCompleteTrigger {
                        //gameOverSuccess()
                    }
                }
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

            //addTask(task)

            // don't copy this
            addTurnCycleTrigger(4).onTrigger {
                queueAddUnitAction(enemyUnits.last { it.stage == null })
            }
            addTurnCycleTrigger(5).onTrigger {
                queueAddUnitAction(enemyUnits.last { it.stage == null })
            }
            addTurnCycleTrigger(6).onTrigger {
                queueAddUnitAction(enemyUnits.last { it.stage == null })
            }
            addTurnCycleTrigger(7).onTrigger {
                queueAddUnitAction(enemyUnits.last { it.stage == null })
            }
            addTurnCycleTrigger(8).onTrigger {
                enemyUnits.filter { it.stage == null }.forEach { queueAddUnitAction(it) }
            }
        }
    }
}