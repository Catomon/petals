package ctmn.petals.story.alissa.scenarios

import ctmn.petals.bot.SimpleBot
import ctmn.petals.map.label.LabelActor
import ctmn.petals.player.Player
import ctmn.petals.player.Team
import ctmn.petals.playscreen.*
import ctmn.petals.playscreen.gui.widgets.StoryDialog
import ctmn.petals.playscreen.seqactions.CameraMoveAction
import ctmn.petals.playscreen.tasks.MoveUnitTask
import ctmn.petals.playscreen.triggers.Trigger
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

class Scenario1dot5 : AlissaScenario(name = "Slime 1.5", levelFileName = "level_1.5.map") {

    private val enemyPlayer = Player("Enemy", Player.RED, Team.RED)

    private lateinit var enemy0Label: LabelActor

    private lateinit var spottedEnemyLeader: UnitActor

    init {
        player = alicePlayer
        gameEndCondition = NoEnd()

        players.add(player, enemyPlayer)
    }

    override fun makeScenario(playScreen: PlayScreen) {
        // ai
        val aiBot = SimpleBot(enemyPlayer, playScreen)
        aiBot.simpleAI.permaAgro = true
        playScreen.botManager.add(aiBot)

        //labels
        for (label in playStage.getLabels()) {
            when (label.labelName) {
                "alice" -> {
                    alice.setPosition(label)
                }
                "enemy_0" -> {
                    enemy0Label = label
                }
                "task" -> {
                    playScreen.queueTask(MoveUnitTask(alice, label.tiledX, label.tiledY)
                        .description("Bring Alice to the marked position")
                    ).addOnCompleteTrigger {
                        playScreen.gameOverSuccess()
                    }
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

            queueAddUnitAction(Slime().player(enemyPlayer).leader(2, 1, true).position(enemy0Label), false)
            for (i in 0..3) {
                queueAddUnitAction(SlimeLing().player(enemyPlayer).followerOf(2, true).position(enemy0Label), false)
            }

            addTrigger(object : Trigger() {
                override fun check(delta: Float): Boolean {
                    if (playScreen.localPlayer.id != playScreen.turnManager.currentPlayer.id) return false

                    for (unit in playStage.getUnitsOfPlayer(enemyPlayer)) {
                        if (unit.isLeader && playScreen.fogOfWarManager.isVisible(unit.tiledX, unit.tiledY)) {
                            spottedEnemyLeader = unit

                            return true
                        }
                    }

                    return false
                }
            }).onTrigger {
                queueAction(CameraMoveAction(spottedEnemyLeader.tileCenterX, spottedEnemyLeader.tileCenterY))
                queueDialogAction(StoryDialog.Quote(
                    "Units with a leader have their strength increased\n" +
                            "when they are near their leader."
                ))
                queueDialogAction(StoryDialog.Quote(
                    "If you defeat a leader, the followers will be gone with them."
                ))
            }
        }
    }
}