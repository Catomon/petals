package ctmn.petals.story.alissa.scenarios

import ctmn.petals.ai.SimpleAIBot
import ctmn.petals.gameactors.label.LabelActor
import ctmn.petals.player.Player
import ctmn.petals.player.Team
import ctmn.petals.playscreen.*
import ctmn.petals.playscreen.seqactions.CameraMoveAction
import ctmn.petals.playscreen.seqactions.WaitAction
import ctmn.petals.playscreen.tasks.MoveUnitTask
import ctmn.petals.playscreen.triggers.Trigger
import ctmn.petals.playscreen.triggers.UnitPosRectTrigger
import ctmn.petals.playstage.getLabels
import ctmn.petals.playstage.getUnitsOfPlayer
import ctmn.petals.story.addAliceDiedGameOverTrigger
import ctmn.petals.story.alissa.AlissaScenario
import ctmn.petals.story.gameOverSuccess
import ctmn.petals.story.playScreen
import ctmn.petals.unit.*
import ctmn.petals.unit.actors.Slime
import ctmn.petals.unit.actors.SlimeLing
import ctmn.petals.unit.UnitActor
import ctmn.petals.utils.*

class Scenario2 : AlissaScenario(name = "Slime 2", levelFileName = "level_2.map") {

    private val enemyPlayer = Player("Enemy", Player.RED, Team.RED)

    private lateinit var label0: LabelActor
    private lateinit var label1: LabelActor
    private lateinit var label2: LabelActor
    private lateinit var label3: LabelActor
    private lateinit var triggerLabel: LabelActor

    private lateinit var currentEnemySpawn: LabelActor

    private lateinit var spottedEnemyLeader: UnitActor

    init {
        player = alicePlayer
        gameEndCondition = NoEnd()

        players.add(player, enemyPlayer)
    }

    override fun makeScenario(playScreen: PlayScreen) {

        playScreen.fogOfWarManager.drawFog = true

        // ai
        val aiBot = SimpleAIBot(enemyPlayer, playScreen)
        aiBot.simpleAI.permaAgro = true
        playScreen.aiManager.add(aiBot)

        //labels
        for (label in playStage.getLabels()) {
            when (label.labelName) {
                "alice" -> {
                    alice.setPosition(label)
                }
                "enemy_0" -> {
                    label0 = label
                }
                "enemy_1" -> {
                    label1 = label
                }
                "enemy_2" -> {
                    label2 = label
                }
                "enemy_3" -> {
                    label3 = label
                }
                "trigger" -> {
                    triggerLabel = label
                }
                "task" -> {
                    playScreen.queueTask(MoveUnitTask(alice, label.tiledX, label.tiledY)
                        .description("Bring Alice to the marked position")
                    ).addOnCompleteTrigger {
                        playScreen.gameOverSuccess()
                    }
                    playScreen.queueAction(CameraMoveAction(label.tileCenterX, label.tileCenterY))
                    playScreen.queueAction(WaitAction(1.75f))
                }
            }
        }

        //sequence
        playScreen {
            addAliceDiedGameOverTrigger(alice)

            queueAddUnitAction(alice)

//            queueDialogAction(StoryDialog.Quote(
//                "If it is your first move of the game,\n" +
//                        "you can use summon ability at no cost of Action Points"
//            ))

            currentEnemySpawn = label0

            addTrigger(UnitPosRectTrigger(alice, triggerLabel.tiledX, triggerLabel.tiledY).expandX().expandTop()).trigger {
                queueAddUnitAction(Slime().player(enemyPlayer).leader(2, 1, true).position(label3), true)
                for (i in 0..3) {
                    queueAddUnitAction(SlimeLing().player(enemyPlayer).followerOf(2, true).position(label3), false)
                }
                queueAction(WaitAction(0.5f))
                queueAction {
                    queueAction(CameraMoveAction(alice.tileCenterX, alice.tileCenterY))
                }
            }

//            addTrigger(object : Trigger() {
//                override fun check(delta: Float): Boolean {
//                    if (playScreen.localPlayer.id != playScreen.turnManager.currentPlayer.id) return false
//
//                    for (unit in playStage.getUnitsOfPlayer(enemyPlayer)) {
//                        if (unit.isLeader && playScreen.fogOfWarManager.isVisible(unit.tiledX, unit.tiledY)) {
//                            spottedEnemyLeader = unit
//
//                            return true
//                        }
//                    }
//
//                    return false
//                }
//            }).trigger {
//                //do something
//            }

            addTrigger(object : Trigger() {
                override fun check(delta: Float): Boolean {
                    for (unit in playStage.getUnitsOfPlayer(enemyPlayer)) {
                        if (unit.followerID == 3) return false
                    }

                    return true
                }
            }.trigger {
                spawnEnemy()
            }).dontRemoveOnTrigger()
        }
    }

    private var spawnedSoFar = 0

    private fun spawnEnemy() {
        if (spawnedSoFar > 6) return

        playScreen.playStage.addActor(SlimeLing().player(enemyPlayer).position(currentEnemySpawn).followerOf(3))

        spawnedSoFar++

        when (spawnedSoFar) {
            1 -> currentEnemySpawn = label1
            2 -> currentEnemySpawn = label2
            3 -> currentEnemySpawn = label1
            4 -> currentEnemySpawn = label2
            5 -> currentEnemySpawn = label1
            6 -> currentEnemySpawn = label2
        }
    }
}