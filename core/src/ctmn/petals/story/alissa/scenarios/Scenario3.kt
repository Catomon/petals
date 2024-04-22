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
import ctmn.petals.playscreen.triggers.TurnStartTrigger
import ctmn.petals.playstage.getLabels
import ctmn.petals.playstage.getUnitsOfPlayer
import ctmn.petals.playstage.tiledHeight
import ctmn.petals.story.addAliceDiedGameOverTrigger
import ctmn.petals.story.alissa.AlissaScenario
import ctmn.petals.story.gameOverSuccess
import ctmn.petals.story.playScreen
import ctmn.petals.unit.*
import ctmn.petals.unit.actors.SlimeHuge
import ctmn.petals.unit.actors.SlimeLing
import ctmn.petals.utils.*

class Scenario3 : AlissaScenario(name = "Slime 3", levelFileName = "level_3.map") {

    private val enemyPlayer = Player("Enemy", Player.RED, Team.RED)

    private lateinit var hugeSlimeLabel0: LabelActor
    private lateinit var label1: LabelActor
    private lateinit var label2: LabelActor

    private lateinit var currentEnemySpawn: LabelActor

    private val slimeHuge = SlimeHuge()

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
                "enemy_0" -> {
                    hugeSlimeLabel0 = label
                }
                "enemy_1" -> {
                    label1 = label
                }
                "enemy_2" -> {
                    label2 = label
                }
            }
        }

        //sequence
        playScreen {
            debug = true

            addAliceDiedGameOverTrigger(alice)

            queueAddUnitAction(alice)

            queueDialogAction(StoryDialog.Quote("Every unit has good and bad match-ups,\n" +
                    "Sword Faerie are good against slimes"))

            queueDialogAction(StoryDialog.Quote("Now you can summon Sword Faerie")).addOnCompleteTrigger {
                alice.summoner.units.add(UnitIds.DOLL_SWORD)
            }

            queueTask(MoveUnitTask(alice, alice.tiledX + 1, playStage.tiledHeight() - 2).description("Bring Alissa to the marked position"))

            //slime appearance
            addTrigger(TurnStartTrigger(enemyPlayer)).trigger {
                taskManager.completeTasks()

                slimeAppearance()

                // spawn side map slimes if there are less than 3 slimes on the map
                queueAction {
                    currentEnemySpawn = label1
                    addTrigger(object : Trigger() {
                        override fun check(delta: Float): Boolean {
                            return playStage.getUnitsOfPlayer(enemyPlayer).size <= 3
                        }
                    }.trigger {
                        spawnEnemy()
                    }).dontRemoveOnTrigger()
                }
            }
        }
    }

    private fun PlayScreen.slimeAppearance() {
        queueAddUnitAction(slimeHuge.player(enemyPlayer).position(0, playStage.tiledHeight()).leader(2, 1, true), false).addOnCompleteTrigger {
            actionManager.queueAction(CameraMoveAction(hugeSlimeLabel0.tileCenterX, hugeSlimeLabel0.tileCenterY))
                .addOnCompleteTrigger {
                    addAction(ActorAction(slimeHuge, JumpAction(hugeSlimeLabel0.tileCenterX, hugeSlimeLabel0.tileCenterY)))
                        .addOnCompleteTrigger {

                            // add dialog
                            queueDialogAction(StoryDialog.Quote("*slurp*", slimeHuge))
                            queueDialogAction(StoryDialog.Quote("The big one", alice))

                            // and task
                            addTask(KillUnitTask(slimeHuge).description("Defeat The Huge Slime")).addOnCompleteTrigger {

                                queueDialogAction(StoryDialog.Quote("Long quote asdasda asdasdasd!!!", alice))

                                queueAction { gameOverSuccess() }
                            }
                        }
                }
        }
    }

    private var spawnedSoFar = 0

    private fun spawnEnemy() {
        if (spawnedSoFar > 6) return
        if (!slimeHuge.isAlive() || slimeHuge.stage == null) return

        playScreen.playStage.addActor(SlimeLing().player(enemyPlayer).position(currentEnemySpawn).followerOf(2, true))

        spawnedSoFar++

        when (currentEnemySpawn) {
            label1 -> currentEnemySpawn = label2
            label2 -> currentEnemySpawn = label1
        }
    }
}