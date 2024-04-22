package ctmn.petals.story.alissa.scenarios

import ctmn.petals.actors.actions.JumpAction
import ctmn.petals.ai.SimpleBot
import ctmn.petals.map.label.LabelActor
import ctmn.petals.player.Player
import ctmn.petals.player.Team
import ctmn.petals.playscreen.*
import ctmn.petals.playscreen.gui.widgets.StoryDialog
import ctmn.petals.playscreen.seqactions.ActorAction
import ctmn.petals.playscreen.seqactions.CameraMoveAction
import ctmn.petals.playscreen.tasks.KillUnitTask
import ctmn.petals.playscreen.tasks.MoveUnitTask
import ctmn.petals.playscreen.triggers.Trigger
import ctmn.petals.playscreen.triggers.UnitPosRectTrigger
import ctmn.petals.playstage.*
import ctmn.petals.story.addAliceDiedGameOverTrigger
import ctmn.petals.story.alissa.AlissaScenario
import ctmn.petals.story.gameOverSuccess
import ctmn.petals.story.playScreen
import ctmn.petals.unit.*
import ctmn.petals.unit.actors.FishGuy
import ctmn.petals.utils.*

class FishGuyScenario : AlissaScenario(name = "Fish Guy", levelFileName = "fish_guy.map") {

    private val enemyPlayer = Player("Enemy", Player.RED, Team.RED)

    private lateinit var fishGuyLabel: LabelActor

    private val fishGuy = FishGuy()

    init {
        player = alicePlayer
        gameEndCondition = NoEnd()

        players.add(player, enemyPlayer)
    }

    override fun makeScenario(playScreen: PlayScreen) {

        playScreen.fogOfWarManager.drawFog = false

        // ai
        playScreen.aiManager.add(SimpleBot(enemyPlayer, playScreen))

        //labels
        for (label in playStage.getLabels()) {
            when (label.labelName) {
                "alice" -> {
                    alice.setPosition(label)
                }
                "fish_guy" -> {
                    fishGuyLabel = label
                }
            }
        }

        //sequence
        playScreen {
            debug = true

            addAliceDiedGameOverTrigger(alice)

            queueAddUnitAction(alice)

            queueTask(MoveUnitTask(alice, playStage.tiledWidth() / 2, playStage.tiledHeight() - 2).description("Bring Alissa to the marked position"))

            //appearance
            val triggerLabel = playStage.getLabel("trigger")
            addTrigger(UnitPosRectTrigger(alice, triggerLabel.tiledX, triggerLabel.tiledY).expandX().expandTop()).trigger {
                taskManager.completeTasks()

                slimeAppearance()

                // trigger
                queueAction {
                    addTrigger(object : Trigger() {
                        override fun check(delta: Float): Boolean {
                            return playStage.getUnitsOfPlayer(enemyPlayer).size <= 3
                        }
                    }.trigger {
                        //spawnEnemy()
                    }).dontRemoveOnTrigger()
                }
            }
        }
    }

    private fun PlayScreen.slimeAppearance() {
        queueAddUnitAction(fishGuy.player(enemyPlayer).position(0, playStage.tiledHeight()).leader(2, 1, true), false).addOnCompleteTrigger {
            actionManager.queueAction(CameraMoveAction(fishGuyLabel.tileCenterX, fishGuyLabel.tileCenterY))
                .addOnCompleteTrigger {
                    addAction(ActorAction(fishGuy, JumpAction(fishGuyLabel.tileCenterX - (3).unTiled(), fishGuyLabel.tileCenterY, fishGuyLabel.tileCenterX, fishGuyLabel.tileCenterY)))
                        .addOnCompleteTrigger {

                            // add dialog
                            queueDialogAction(StoryDialog.Quote("*slurp*", fishGuy))
                            queueDialogAction(StoryDialog.Quote("The big one", alice))

                            // and task
                            addTask(KillUnitTask(fishGuy).description("Defeat The Huge Slime")).addOnCompleteTrigger {

                                queueDialogAction(StoryDialog.Quote("Long quote asdasda asdasdasd!!!", alice))

                                queueAction { gameOverSuccess() }
                            }
                        }
                }
        }
    }
}