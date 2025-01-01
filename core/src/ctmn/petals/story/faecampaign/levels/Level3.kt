package ctmn.petals.story.faecampaign.levels

import com.badlogic.gdx.scenes.scene2d.Actor
import ctmn.petals.bot.EasyDuelBot
import ctmn.petals.player.newBluePlayer
import ctmn.petals.player.newRedPlayer
import ctmn.petals.playscreen.*
import ctmn.petals.playscreen.events.ActionCompletedEvent
import ctmn.petals.playscreen.gui.widgets.CharactersPanel
import ctmn.petals.playscreen.gui.widgets.StoryDialog
import ctmn.petals.playscreen.seqactions.AttackAction
import ctmn.petals.playscreen.tasks.EliminateAllEnemyUnitsTask
import ctmn.petals.playscreen.tasks.KeepPlayerUnitsAlive
import ctmn.petals.playscreen.triggers.Trigger
import ctmn.petals.playstage.getUnitsOfPlayer
import ctmn.petals.story.Scenario
import ctmn.petals.story.playScreen
import ctmn.petals.unit.UnitIds

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
            val fairyHelper = guiStage.charactersPanel.findActor<Actor>(CharactersPanel.CHARACTER_HELPER_FAIRY)

            queueDialogAction(
                StoryDialog.Quote("Each unit type has its own match up bonuses", fairyHelper)
            ).addOnCompleteTrigger {
                guiStage.actorHighlighter.targetActors.add(guiStage.bookButton)
            }

            queueDialogAction(
                StoryDialog.Quote(
                    "You will be able to access unit info\nin the book by pressing the button above",
                    fairyHelper
                )
            ).addOnCompleteTrigger {
                guiStage.actorHighlighter.targetActors.remove(guiStage.bookButton)
            }

            queueDialogAction(
                StoryDialog.Quote(
                    "I will observe your battles and update it\nif I get any new info about units and their matchups",
                    fairyHelper
                )
            )

            addTrigger(object : Trigger() {
                var isDone = false

                override fun onAdded() {
                    playStage.addListener {
                        if (isDone) return@addListener false
                        if (it is ActionCompletedEvent) {
                            if (it.action is AttackAction) {
                                if (it.action.attackerUnit.selfName == UnitIds.DOLL_PIKE) {
                                    if (it.action.targetUnit.selfName == UnitIds.GOBLIN_BOAR) {
                                        isDone = true
                                    }
                                }
                            }
                        }

                        false
                    }
                }

                override fun check(delta: Float): Boolean {
                    return isDone
                }

            }).onTrigger {
                queueDialogAction(
                    StoryDialog.Quote(
                        "It appears that pikes,\nwhen going against boar riders,\nare doing more damage than expected while taking less.",
                        fairyHelper
                    )
                )

                queueDialogAction(
                    StoryDialog.Quote(
                        "I have added this information to the book.",
                        fairyHelper
                    )
                )
            }

            addTask(EliminateAllEnemyUnitsTask().description("Eliminate all enemy units")).addOnCompleteTrigger {
                //gameOverSuccess()
            }

            addTask(task)
        }
    }
}