package ctmn.petals.story.faecampaign.levels

import com.badlogic.gdx.utils.Array
import ctmn.petals.Const
import ctmn.petals.bot.EasyDuelBot
import ctmn.petals.player.SpeciesUnitNotFoundExc
import ctmn.petals.player.fairyUnits
import ctmn.petals.player.newBluePlayer
import ctmn.petals.player.newRedPlayer
import ctmn.petals.playscreen.*
import ctmn.petals.playscreen.commands.BuildBaseCommand
import ctmn.petals.playscreen.gui.widgets.StoryDialog
import ctmn.petals.playscreen.tasks.*
import ctmn.petals.playscreen.triggers.PlayerHasNoUnits
import ctmn.petals.playscreen.triggers.TurnStartTrigger
import ctmn.petals.playscreen.triggers.UnitsDiedTrigger
import ctmn.petals.playstage.getCapturablesOf
import ctmn.petals.playstage.getUnit
import ctmn.petals.playstage.getUnitsOfPlayer
import ctmn.petals.story.Scenario
import ctmn.petals.story.playScreen
import ctmn.petals.unit.*
import ctmn.petals.unit.actors.fairies.FairySower

class Level5 : Scenario("lv_5", "level_capture") {

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
            it.add(fairyUnits.units.find { it.selfName == UnitIds.DOLL_SWORD } ?: throw SpeciesUnitNotFoundExc())
            it.add(fairyUnits.units.find { it.selfName == UnitIds.DOLL_PIKE } ?: throw SpeciesUnitNotFoundExc())
//            it.add(fairyUnits.units.find { it.selfName == UnitIds.DOLL_AXE } ?: throw SpeciesUnitNotFoundExc())
        }

        playScreen {
            queueDialogAction(
                StoryDialog.Quote(
                    "To get units, you need to create a base using a worker unit"
                ),
                StoryDialog.Quote(
                    "A base can be claimed only on plain terrain and shallow water." +
                            "It costs ${Const.BASE_BUILD_COST} and takes ${Const.BASE_BUILD_TIME} turns to make."
                ),
                StoryDialog.Quote(
                    "Position Sower Fairy at the marked location and press 'Create Base' to begin planting a seed."
                )
            ).addOnCompleteTrigger {
                val fairySower = playStage.getUnit<FairySower>()!!
                guiStage.selectUnit(fairySower)
                queueTask(
                    MoveUnitTask(
                        fairySower,
                        fairySower.tiledX + 2,
                        fairySower.tiledY + 1,
                        true
                    ).description("Move Fairy Sower")
                ).addOnCompleteTrigger {
                    queueTask(ExecuteCommandTask(BuildBaseCommand::class, true).description("Create a base"))
                    addTurnCycleTrigger(3, players[0]).onTrigger {
                        queueDialogAction(
                            StoryDialog.Quote(
                                "Buy Sower Fairies to claim bases and capture crystal tiles."
                            ),
                            StoryDialog.Quote(
                                "When you capture crystal tiles,\n" +
                                        "they provide you with a certain amount of crystals each turn."
                            ),
                            StoryDialog.Quote(
                                "Use these crystals to create more units."
                            )
                        ).addOnCompleteTrigger {
                            addTask(BuyUnitsTask("Fairy Sower", UnitIds.DOLL_SOWER, 2))
                            addTask(CaptureCrystalsTask()).addOnCompleteTrigger {
                                queueDialogAction(
                                    StoryDialog.Quote(
                                    "Each crystal tile can provide up to ${Const.CRYSTALS_CLUSTER} crystals.\n"
                                ))
                            }
                            addTask(EliminateAllEnemyUnitsTask(enemyUnits).description("Repel the enemy's attack")).addOnCompleteTrigger {
                                //gameOverSuccess()
                            }
                        }
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
                queueAddUnitAction(enemyUnits.last { it.isAlive() && it.stage == null })
            }
            addTurnCycleTrigger(5).onTrigger {
                queueAddUnitAction(enemyUnits.last { it.isAlive() && it.stage == null })
            }
            addTurnCycleTrigger(6).onTrigger {
                queueAddUnitAction(enemyUnits.last { it.isAlive() && it.stage == null })
            }
            addTurnCycleTrigger(7).onTrigger {
                queueAddUnitAction(enemyUnits.last { it.isAlive() && it.stage == null })
            }
            addTurnCycleTrigger(8).onTrigger {
                enemyUnits.filter { it.isAlive() && it.stage == null }.forEach { queueAddUnitAction(it) }
            }
        }
    }
}