package ctmn.petals.story.alissa.scenarios

import ctmn.petals.ai.SimpleBot
import ctmn.petals.player.Player
import ctmn.petals.player.Team
import ctmn.petals.playscreen.*
import ctmn.petals.playscreen.gui.widgets.StoryDialog
import ctmn.petals.playscreen.seqactions.CameraMoveAction
import ctmn.petals.playscreen.seqactions.WaitAction
import ctmn.petals.playscreen.tasks.MoveUnitTask
import ctmn.petals.playstage.getLabels
import ctmn.petals.story.addAliceDiedGameOverTrigger
import ctmn.petals.story.alissa.AlissaScenario
import ctmn.petals.story.gameOverSuccess
import ctmn.petals.story.playScreen
import ctmn.petals.unit.actors.*
import ctmn.petals.unit.followerOf
import ctmn.petals.unit.leader
import ctmn.petals.unit.player
import ctmn.petals.unit.position
import ctmn.petals.utils.*

class Scenario7 : AlissaScenario(name = "Evil Trees 2", levelFileName = "level_7.map") {

    private val enemyPlayer = Player("Enemy", Player.RED, Team.RED)

    private val cherie = Cherie()

    init {
        player = alicePlayer
        gameEndCondition = NoEnd()
        players.add(player, enemyPlayer)

        unitsToSaveProgressOf.add(cherie)
    }

    override fun makeScenario(playScreen: PlayScreen) {

        playScreen.fogOfWarManager.drawFog = true

        // ai
        playScreen.aiManager.add(SimpleBot(enemyPlayer, playScreen).apply {
            simpleAI.permaAgro = false
            simpleAI.roamingIfNoAgro = false
            simpleAI.agroRange = 8
        })

        //labels
        for (label in playStage.getLabels()) {
            when (label.labelName) {
                "alice" -> {
                    label.addUnit(alice)
                }
                "ally" -> {
                    label.addUnit(cherie.player(alicePlayer).leader(2, 2, true).position(label))
                    for (i in 0..3) {
                        label.addUnit(CherieSpearman().player(alicePlayer).followerOf(2).position(label))
                    }
                }
                "enemy_0" -> {
                    //label.addUnit(EvilTree().player(enemyPlayer).leader(3, 2, true).position(label))
                    for (i in 0..4) {
                        label.addUnit(EvilTree().player(enemyPlayer).followerOf(3).position(label))
                    }
                }
                "enemy_1" -> {
                    //label.addUnit(EvilTree().player(enemyPlayer).leader(4, 2, true).position(label))
                    for (i in 0..3) {
                        label.addUnit(EvilTree().player(enemyPlayer).followerOf(4).position(label))
                    }
                }
                "enemy_2" -> {
                    label.addUnit(EvilTree().player(enemyPlayer).leader(5, 2, true).position(label))
                    for (i in 0..3) {
                        label.addUnit(EvilTree().player(enemyPlayer).followerOf(5).position(label))
                    }
                }
                "enemy_3" -> {
                    label.addUnit(Slime().player(enemyPlayer).leader(6, 2, true).position(label))
                    for (i in 0..5) {
                        label.addUnit(SlimeLing().player(enemyPlayer).followerOf(6).position(label))
                    }
                }
                "enemy_4" -> {
                    label.addUnit(EvilTree().player(enemyPlayer).leader(7, 2, true).position(label))
                    for (i in 0..5) {
                        label.addUnit(EvilTree().player(enemyPlayer).followerOf(7).position(label))
                    }
                }
                "task" -> {
                    playScreen.queueTask(
                        MoveUnitTask(alice, label.tiledX, label.tiledY)
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

            queueDialogAction(
                StoryDialog.Quote("dfgdf", cherie),
                StoryDialog.Quote("dfgd", alice),
            )
        }
    }
}